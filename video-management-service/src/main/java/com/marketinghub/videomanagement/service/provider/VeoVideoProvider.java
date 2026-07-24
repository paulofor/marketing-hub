package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Adapter direto para renderizar vídeos comerciais usando VEO via Gemini API. */
@Component
@ConditionalOnProperty(prefix = "video.providers.veo", name = "enabled", havingValue = "true")
public class VeoVideoProvider implements VideoProvider {
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 25 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    /** Inicializa o provider com configuração VEO, mapper JSON e WebClient. */
    public VeoVideoProvider(VideoManagementProperties properties,
                            ObjectMapper objectMapper,
                            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(resolveBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_VIDEO_DOWNLOAD_BYTES))
                        .build())
                .build();
    }

    /** Verifica se o job de render pertence ao provider VEO. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        if (!StringUtils.hasText(providerName)) {
            return false;
        }
        return properties.getProviders().getVeo().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Envia o prompt para VEO, aguarda a operação finalizar e baixa o MP4 gerado. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        requireApiKey();
        SalesVideoScript script = ensureScript(profile);
        progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING, "Enviando prompt para VEO");

        String operationName = submitRender(job, profile, script);
        progressCallback.onProgress(30, SalesVideoStatus.VIDEO_PROCESSING,
                "VEO aceitou a operação: " + operationName);

        JsonNode finalStatus = waitUntilDone(operationName, progressCallback);
        JsonNode error = finalStatus.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "VEO retornou erro: " + error.path("message").asText(error.toString()));
        }

        String videoUri = finalStatus.path("response")
                .path("generateVideoResponse")
                .path("generatedSamples")
                .path(0)
                .path("video")
                .path("uri")
                .asText(null);
        if (!StringUtils.hasText(videoUri)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "VEO não retornou URI do vídeo gerado");
        }

        progressCallback.onProgress(85, SalesVideoStatus.VIDEO_PROCESSING, "Baixando MP4 gerado pelo VEO");
        ProviderFile video = downloadVideo(job, videoUri);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "VEO");
        metadata.put("provider_job_id", operationName);
        metadata.put("model", properties.getProviders().getVeo().getModel());
        metadata.put("aspect_ratio", properties.getProviders().getVeo().getAspectRatio());
        metadata.put("resolution", properties.getProviders().getVeo().getResolution());
        metadata.put("duration_seconds", properties.getProviders().getVeo().getDurationSeconds());
        metadata.put("cost_usd", estimateCostUsd(
                properties.getProviders().getVeo().getModel(),
                properties.getProviders().getVeo().getDurationSeconds(),
                properties.getProviders().getVeo().getResolution()));
        metadata.put("pricing_source", "Google Gemini API pricing: Veo video generation billed per generated second");
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));

        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "VEO finalizado com MP4 disponível");
        return new ProviderArtifacts(operationName, video, null, null, metadata);
    }

    /** Cria a operação long-running no endpoint predictLongRunning do VEO. */
    private String submitRender(SalesVideoJob job,
                                SalesVideoProfile profile,
                                SalesVideoScript script) {
        VideoManagementProperties.Veo config = properties.getProviders().getVeo();
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("prompt", buildVeoPrompt(job, profile, script));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("aspectRatio", config.getAspectRatio());
        parameters.put("resolution", config.getResolution());
        parameters.put("personGeneration", config.getPersonGeneration());
        parameters.put("durationSeconds", config.getDurationSeconds());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instances", java.util.List.of(instance));
        payload.put("parameters", parameters);

        JsonNode response = authorized(webClient.post()
                        .uri("/models/{model}:predictLongRunning", config.getModel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        String operationName = response != null ? response.path("name").asText(null) : null;
        if (!StringUtils.hasText(operationName)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "VEO não retornou o nome da operação para o job " + job.id());
        }
        return operationName;
    }

    /** Aguarda a operação assíncrona do VEO concluir dentro do limite configurado. */
    private JsonNode waitUntilDone(String operationName,
                                   ProgressCallback progressCallback) {
        VideoManagementProperties.Veo config = properties.getProviders().getVeo();
        for (int attempt = 1; attempt <= config.getMaxPollAttempts(); attempt++) {
            JsonNode status = authorized(webClient.get().uri("/" + operationName))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (status != null && status.path("done").asBoolean(false)) {
                return status;
            }
            int progress = Math.min(80, 30 + attempt);
            progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                    "VEO ainda processando (tentativa %d/%d)".formatted(attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão do VEO");
    }

    /** Baixa o arquivo final informado pela Gemini API usando a mesma chave de autenticação. */
    private ProviderFile downloadVideo(SalesVideoJob job, String videoUri) {
        ResponseEntity<byte[]> response = authorized(webClient.get().uri(videoUri))
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Download vazio do vídeo VEO");
        }
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null || !VIDEO_MP4.isCompatibleWith(contentType) || !looksLikeMp4(content)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Download do VEO não retornou MP4 válido; contentType=%s bytes=%d"
                            .formatted(contentType, content.length));
        }
        return new ProviderFile("sales-video-" + job.id() + "-veo.mp4",
                VIDEO_MP4,
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                content);
    }

    /** Monta um prompt de vídeo a partir do roteiro aprovado e do perfil comercial. */
    private String buildVeoPrompt(SalesVideoJob job, SalesVideoProfile profile, SalesVideoScript script) {
        String storyboard = StringUtils.hasText(script.storyboardJson()) ? script.storyboardJson() : "[]";
        JsonNode metadata = readMetadata(job);
        String characterPrompt = metadata.path("characterImagePrompt").asText("");
        String characterReferenceUrl = metadata.path("characterImageReferenceUrl").asText("");
        String visualDirectives = visualProviderDirectives(metadata);
        return """
                Vertical short-form sales video for a digital product.
                Language: %s.
                Title: %s.
                Persona: %s.
                Voice style: %s.
                Character reference image URL: %s.
                Character image prompt: %s.
                Hook: %s.
                Script: %s.
                CTA: %s.
                Storyboard JSON: %s.
                Visual direction: cinematic but direct-response oriented, keep the same character identity and style across scenes, clear product promise, human emotion, readable pacing, native audio, no fake UI claims, no impossible guarantees.
                Provider-specific visual directives: %s.
                """.formatted(
                nullToDefault(profile.language(), "pt-BR"),
                nullToDefault(profile.title(), "Sales video"),
                nullToDefault(profile.personaName(), "target customer"),
                nullToDefault(profile.voiceStyle(), "confident"),
                nullToDefault(characterReferenceUrl, "not available"),
                nullToDefault(characterPrompt, "not available"),
                nullToDefault(script.hookText(), ""),
                script.scriptText(),
                nullToDefault(script.ctaText(), ""),
                storyboard,
                visualDirectives);
    }

    /** Extrai diretivas visuais do metadata para orientar nitidez, luz e composição no provider. */
    private String visualProviderDirectives(JsonNode metadata) {
        String directives = metadata.path("visual_provider_directives").asText("");
        if (StringUtils.hasText(directives)) {
            return directives.trim();
        }
        return "Sharp image, crisp focus, stable exposure, constant natural light, no haze, no blur and no flickering.";
    }

    /** Calcula o custo oficial aproximado do VEO em USD por segundo gerado. */
    private BigDecimal estimateCostUsd(String model, Integer durationSeconds, String resolution) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return null;
        }
        BigDecimal pricePerSecond = pricePerSecondUsd(model, resolution);
        return pricePerSecond.multiply(BigDecimal.valueOf(durationSeconds.longValue()))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /** Resolve o preço por segundo do VEO conforme modelo e resolução. */
    private BigDecimal pricePerSecondUsd(String model, String resolution) {
        String normalizedModel = normalize(model).toLowerCase(Locale.ROOT);
        if (normalizedModel.contains("veo-2")) {
            return new BigDecimal("0.35");
        }
        if (normalizedModel.contains("fast")) {
            return veoFastPrice(resolution);
        }
        if (normalizedModel.contains("lite")) {
            return normalize(resolution).toLowerCase(Locale.ROOT).contains("1080")
                    ? new BigDecimal("0.08")
                    : new BigDecimal("0.05");
        }
        String normalizedResolution = normalize(resolution).toLowerCase(Locale.ROOT);
        return normalizedResolution.contains("4k") || normalizedResolution.contains("2160")
                ? new BigDecimal("0.60")
                : new BigDecimal("0.40");
    }

    /** Resolve preço por segundo do VEO Fast conforme resolução. */
    private BigDecimal veoFastPrice(String resolution) {
        String normalizedResolution = normalize(resolution).toLowerCase(Locale.ROOT);
        if (normalizedResolution.contains("4k") || normalizedResolution.contains("2160")) {
            return new BigDecimal("0.30");
        }
        if (normalizedResolution.contains("1080")) {
            return new BigDecimal("0.12");
        }
        return new BigDecimal("0.10");
    }

    /** Garante que o perfil possui roteiro aprovado para renderização. */
    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização VEO");
        }
        return script;
    }

    /** Aplica a chave Gemini ao request HTTP. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        return request.header("x-goog-api-key", resolveApiKey());
    }

    /** Bloqueia execução real sem chave Gemini configurada. */
    private void requireApiKey() {
        if (!StringUtils.hasText(resolveApiKey())) {
            throw new VideoProviderException("PROVIDER_AUTH_ERROR", "GEMINI_API_KEY não configurada para VEO");
        }
    }

    /** Resolve a chave Gemini por valor direto ou arquivo de secret montado no container. */
    private String resolveApiKey() {
        VideoManagementProperties.Veo config = properties.getProviders().getVeo();
        if (StringUtils.hasText(config.getApiKey())) {
            return config.getApiKey().trim();
        }
        if (!StringUtils.hasText(config.getApiKeyFile())) {
            return null;
        }
        try {
            String key = Files.readString(Path.of(config.getApiKeyFile().trim())).trim();
            return StringUtils.hasText(key) ? key : null;
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível ler GEMINI_API_KEY_FILE para VEO", ex);
        }
    }

    /** Lê os metadados comerciais do job para preservar personagem e referência visual no VEO. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (job == null || !StringUtils.hasText(job.metadataJson())) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (Exception ex) {
            throw new VideoProviderException("PROVIDER_INVALID_REQUEST", "metadataJson inválido para job VEO " + job.id(), ex);
        }
    }

    /** Resolve a base URL configurada para Gemini API. */
    private String resolveBaseUrl() {
        return properties.getProviders().getVeo().getBaseUrl().toString();
    }

    /** Normaliza nomes de provider para comparação estável. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /** Substitui valores vazios por padrão textual seguro para o prompt. */
    private String nullToDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /** Verifica assinatura mínima de container MP4 para não salvar JSON/erro como vídeo. */
    private boolean looksLikeMp4(byte[] content) {
        if (content.length < 12) {
            return false;
        }
        return content[4] == 'f'
                && content[5] == 't'
                && content[6] == 'y'
                && content[7] == 'p';
    }

    /** Aguarda entre tentativas de polling e preserva interrupção da thread. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("Thread interrompida durante polling do VEO", ex);
        }
    }
}
