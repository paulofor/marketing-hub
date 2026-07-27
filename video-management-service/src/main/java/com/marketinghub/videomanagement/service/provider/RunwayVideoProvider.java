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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

/** Responsabilidade: renderizar vídeos comerciais curtos pela Runway para jobs do Marketing Hub. */
@Component
@ConditionalOnProperty(prefix = "video.providers.runway", name = "enabled", havingValue = "true")
public class RunwayVideoProvider implements VideoProvider {
    private static final Logger log = LoggerFactory.getLogger(RunwayVideoProvider.class);
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 100 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final WebClient downloadWebClient;

    /** Inicializa o provider Runway com configuração, mapper JSON e clients HTTP. */
    public RunwayVideoProvider(VideoManagementProperties properties,
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
        this.downloadWebClient = webClientBuilder.clone()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_VIDEO_DOWNLOAD_BYTES))
                        .build())
                .build();
    }

    /** Verifica se o job de render pertence ao provider Runway. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        if (!StringUtils.hasText(providerName)) {
            return false;
        }
        return properties.getProviders().getRunway().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Envia o prompt para Runway, aguarda conclusão, baixa o MP4 e devolve artefatos auditáveis. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        requireApiKey();
        SalesVideoScript script = ensureScript(profile);
        progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING, "Enviando prompt para Runway");

        Map<String, Object> payload = buildPayload(job, profile, script);
        String taskId = submitRender(job, payload);
        progressCallback.onProgress(30, SalesVideoStatus.VIDEO_PROCESSING, "Runway aceitou o taskId: " + taskId);

        JsonNode finalStatus = waitUntilCompleted(taskId, progressCallback);
        String videoUrl = resolveVideoUrl(finalStatus);
        if (!StringUtils.hasText(videoUrl)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Runway não retornou URL do vídeo gerado");
        }

        progressCallback.onProgress(85, SalesVideoStatus.VIDEO_PROCESSING, "Baixando MP4 gerado pela Runway");
        ProviderFile video = downloadVideo(job, videoUrl);
        Map<String, Object> metadata = metadata(job, taskId, payload, finalStatus);
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Runway finalizada com MP4 disponível");
        return new ProviderArtifacts(taskId, video, null, null, metadata);
    }

    /** Cria a tarefa image-to-video ou text-to-video na Runway. */
    private String submitRender(SalesVideoJob job, Map<String, Object> payload) {
        String path = properties.getProviders().getRunway().getCreatePath();
        log.info("Chamando Runway para criar vídeo; jobId={} url={} request={}", job.id(), resolveBaseUrl() + path, payload);
        JsonNode response;
        try {
            response = authorized(webClient.post()
                            .uri(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw providerHttpError(job, "criação", path, ex);
        }
        log.info("Resposta Runway create; jobId={} response={}", job.id(), response);
        String taskId = firstText(response, "/id", "/taskId", "/task_id");
        if (!StringUtils.hasText(taskId)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Runway não retornou id de tarefa para o job " + job.id());
        }
        return taskId;
    }

    /** Aguarda a tarefa Runway chegar em sucesso ou falha objetiva. */
    private JsonNode waitUntilCompleted(String taskId, ProgressCallback progressCallback) {
        VideoManagementProperties.Runway config = properties.getProviders().getRunway();
        for (int attempt = 1; attempt <= config.getMaxPollAttempts(); attempt++) {
            String path = config.getStatusPathTemplate().replace("{taskId}", taskId);
            log.info("Consultando Runway; taskId={} url={}", taskId, resolveBaseUrl() + path);
            JsonNode status;
            try {
                status = authorized(webClient.get().uri(path))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
            } catch (WebClientResponseException ex) {
                throw providerHttpError(null, "consulta", path, ex);
            }
            log.info("Resposta Runway status; taskId={} response={}", taskId, status);
            String taskStatus = normalize(firstText(status, "/status"));
            if (isSuccess(taskStatus)) {
                return status;
            }
            if (isFailure(taskStatus)) {
                throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                        "Runway falhou: " + readFailure(status));
            }
            int progress = Math.min(80, 30 + attempt);
            progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                    "Runway ainda processando (tentativa %d/%d)".formatted(attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão da Runway");
    }

    /** Monta payload aceito pela Runway com prompt comercial e imagem de referência quando existir. */
    private Map<String, Object> buildPayload(SalesVideoJob job, SalesVideoProfile profile, SalesVideoScript script) {
        VideoManagementProperties.Runway config = properties.getProviders().getRunway();
        JsonNode metadata = readMetadata(job);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.getModel());
        payload.put("promptText", buildPrompt(job, profile, script, metadata));
        payload.put("ratio", config.getRatio());
        payload.put("duration", config.getDurationSeconds());
        String promptImage = firstText(metadata,
                "/characterImageReferenceUrl",
                "/image_to_video/reference_image_url",
                "/image_to_video/source_image_url",
                "/promptImage");
        if (StringUtils.hasText(promptImage)) {
            payload.put("promptImage", promptImage);
        }
        return payload;
    }

    /** Monta prompt comercial genérico a partir do roteiro aprovado e metadados do job. */
    private String buildPrompt(SalesVideoJob job,
                               SalesVideoProfile profile,
                               SalesVideoScript script,
                               JsonNode metadata) {
        String visualDirectives = visualProviderDirectives(metadata);
        String scenes = metadata.path("assembly_plan").path("scenes").isMissingNode()
                ? "Recognizable pain, plausible mechanism, personal value and CTA."
                : metadata.path("assembly_plan").path("scenes").toString();
        return """
                Vertical short-form sales video for a digital product.
                Language: %s.
                Title: %s.
                Audience/persona: %s.
                Communication style: %s.
                Approved hook: %s.
                Approved script context: %s.
                Approved CTA: %s.
                Scene plan: %s.
                Provider-specific visual directives: %s.
                Keep the scene natural, concrete and commercially useful. Show a human situation, the felt pain, a plausible mechanism and a light CTA.
                Avoid embedded text, logos, distorted hands, haze, blur, flicker, body-focused framing, seductive posing and luxury ostentation.
                """.formatted(
                nullToDefault(profile.language(), "pt-BR"),
                nullToDefault(profile.title(), "Sales video"),
                nullToDefault(profile.personaName(), "target customer"),
                nullToDefault(profile.personaStyle(), "natural and direct"),
                nullToDefault(script.hookText(), ""),
                script.scriptText(),
                nullToDefault(script.ctaText(), ""),
                scenes,
                visualDirectives);
    }

    /** Extrai diretivas visuais enviadas pelo Marketing Hub. */
    private String visualProviderDirectives(JsonNode metadata) {
        String directives = metadata.path("visual_provider_directives").asText("");
        if (StringUtils.hasText(directives)) {
            return directives.trim();
        }
        return "Sharp image, stable exposure, constant natural daylight, clear face and practical action.";
    }

    /** Baixa o MP4 final retornado pela Runway. */
    private ProviderFile downloadVideo(SalesVideoJob job, String videoUrl) {
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(URI.create(videoUrl))
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Download vazio do vídeo Runway");
        }
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null || !VIDEO_MP4.isCompatibleWith(contentType) || !looksLikeMp4(content)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Download da Runway não retornou MP4 válido; contentType=%s bytes=%d"
                            .formatted(contentType, content.length));
        }
        return new ProviderFile("sales-video-" + job.id() + "-runway.mp4",
                VIDEO_MP4,
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                content);
    }

    /** Consolida metadados de auditoria do render Runway. */
    private Map<String, Object> metadata(SalesVideoJob job,
                                         String taskId,
                                         Map<String, Object> request,
                                         JsonNode finalStatus) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "RUNWAY");
        metadata.put("provider_job_id", taskId);
        metadata.put("model", properties.getProviders().getRunway().getModel());
        metadata.put("ratio", properties.getProviders().getRunway().getRatio());
        metadata.put("duration_seconds", properties.getProviders().getRunway().getDurationSeconds());
        metadata.put("cost_usd", estimateCostUsd());
        metadata.put("pricing_source", "Runway API charges credits per second by model; see official pricing");
        metadata.put("request", request);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Calcula custo aproximado para Gen-4.5 com créditos de US$0,01. */
    private BigDecimal estimateCostUsd() {
        int seconds = Math.max(1, properties.getProviders().getRunway().getDurationSeconds());
        BigDecimal creditsPerSecond = "gen4_turbo".equalsIgnoreCase(properties.getProviders().getRunway().getModel())
                ? new BigDecimal("5")
                : new BigDecimal("12");
        return creditsPerSecond.multiply(BigDecimal.valueOf(seconds)).multiply(new BigDecimal("0.01"));
    }

    /** Extrai URL de vídeo do formato padrão de task output da Runway. */
    private String resolveVideoUrl(JsonNode status) {
        String direct = firstText(status, "/output/0", "/outputs/0", "/video_url", "/videoUrl", "/url");
        return StringUtils.hasText(direct) ? direct : null;
    }

    /** Converte erro HTTP da Runway em falha auditável com corpo sanitizado. */
    private VideoProviderException providerHttpError(SalesVideoJob job,
                                                     String operation,
                                                     String path,
                                                     WebClientResponseException ex) {
        String body = sanitizeProviderBody(ex.getResponseBodyAsString());
        log.warn("Runway retornou erro HTTP; jobId={} operation={} status={} url={} responseBody={}",
                job == null ? null : job.id(),
                operation,
                ex.getStatusCode().value(),
                resolveBaseUrl() + path,
                body,
                ex);
        String code = ex.getStatusCode().value() == 429 ? "PROVIDER_RATE_LIMIT" : "PROVIDER_RENDER_FAILED";
        return new VideoProviderException(code,
                "Runway retornou HTTP %d em %s: %s"
                        .formatted(ex.getStatusCode().value(), operation, body),
                ex);
    }

    /** Limita corpo de erro externo para log e retorno sem vazar conteúdo excessivo. */
    private String sanitizeProviderBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "sem corpo";
        }
        String normalized = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 1200 ? normalized.substring(0, 1200) : normalized;
    }

    /** Resolve primeiro texto existente nos JSON pointers informados. */
    private String firstText(JsonNode node, String... pointers) {
        if (node == null) {
            return null;
        }
        for (String pointer : pointers) {
            JsonNode value = node.at(pointer);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText(null);
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    /** Identifica status de sucesso da Runway. */
    private boolean isSuccess(String status) {
        return "SUCCEEDED".equals(status) || "SUCCESS".equals(status) || "COMPLETED".equals(status);
    }

    /** Identifica status de falha da Runway. */
    private boolean isFailure(String status) {
        return "FAILED".equals(status) || "FAIL".equals(status) || "CANCELED".equals(status) || "CANCELLED".equals(status);
    }

    /** Lê motivo de falha retornado pela Runway. */
    private String readFailure(JsonNode status) {
        String message = firstText(status,
                "/failure",
                "/failureCode",
                "/failure_code",
                "/error/message",
                "/message");
        return StringUtils.hasText(message) ? message : String.valueOf(status);
    }

    /** Garante que o perfil possui roteiro aprovado para renderização. */
    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização Runway");
        }
        return script;
    }

    /** Bloqueia execução real sem chave Runway configurada. */
    private void requireApiKey() {
        if (!StringUtils.hasText(resolveApiKey())) {
            throw new VideoProviderException("PROVIDER_AUTH_ERROR", "RUNWAY_API_KEY não configurada para Runway");
        }
    }

    /** Aplica autenticação Bearer e versão oficial nas chamadas Runway. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveApiKey());
        request.header("X-Runway-Version", properties.getProviders().getRunway().getApiVersion());
        request.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    /** Resolve a chave Runway por valor direto ou arquivo de secret montado no container. */
    private String resolveApiKey() {
        VideoManagementProperties.Runway config = properties.getProviders().getRunway();
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
            throw new UncheckedIOException("Não foi possível ler RUNWAY_API_KEY_FILE para Runway", ex);
        }
    }

    /** Lê metadados comerciais do job para montar prompt e auditoria. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (job == null || !StringUtils.hasText(job.metadataJson())) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (IOException ex) {
            throw new VideoProviderException("PROVIDER_INVALID_REQUEST",
                    "metadataJson inválido para job Runway " + job.id(), ex);
        }
    }

    /** Resolve base URL configurada para a API Runway. */
    private String resolveBaseUrl() {
        return properties.getProviders().getRunway().getBaseUrl().toString();
    }

    /** Verifica assinatura mínima de container MP4. */
    private boolean looksLikeMp4(byte[] content) {
        if (content.length < 12) {
            return false;
        }
        return content[4] == 'f'
                && content[5] == 't'
                && content[6] == 'y'
                && content[7] == 'p';
    }

    /** Normaliza textos de provider e status. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /** Substitui valores vazios por padrão textual seguro para o prompt. */
    private String nullToDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /** Aguarda entre tentativas de polling preservando interrupção da thread. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("Thread interrompida durante polling da Runway", ex);
        }
    }
}
