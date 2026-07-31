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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

/** Responsabilidade: renderizar vídeos comerciais com avatar e narração sincronizada pela HeyGen. */
@Component
@ConditionalOnProperty(prefix = "video.providers.heygen", name = "enabled", havingValue = "true")
public class HeyGenVideoProvider implements VideoProvider {
    private static final Logger log = LoggerFactory.getLogger(HeyGenVideoProvider.class);
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 150 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final WebClient downloadWebClient;

    /** Inicializa o provider HeyGen com configuração, JSON mapper e clients HTTP. */
    public HeyGenVideoProvider(VideoManagementProperties properties,
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

    /** Verifica se o job de render pertence ao provider HeyGen. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        if (!StringUtils.hasText(providerName)) {
            return false;
        }
        return properties.getProviders().getHeygen().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Cria o vídeo na HeyGen, aguarda conclusão, baixa o MP4 e devolve artefatos auditáveis. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        requireApiKey();
        SalesVideoScript script = ensureScript(profile);
        JsonNode metadata = readMetadata(job);
        progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING, "Enviando roteiro para HeyGen");

        Map<String, Object> payload = buildPayload(job, profile, script, metadata);
        String videoId = submitRender(job, payload);
        progressCallback.onProgress(30, SalesVideoStatus.VIDEO_PROCESSING, "HeyGen aceitou o videoId: " + videoId);

        JsonNode finalStatus = waitUntilCompleted(videoId, progressCallback);
        String videoUrl = resolveVideoUrl(finalStatus);
        if (!StringUtils.hasText(videoUrl)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "HeyGen não retornou URL do vídeo gerado");
        }

        progressCallback.onProgress(85, SalesVideoStatus.VIDEO_PROCESSING, "Baixando MP4 gerado pela HeyGen");
        ProviderFile video = downloadVideo(job, videoUrl);
        Map<String, Object> providerMetadata = metadata(job, videoId, payload, finalStatus);
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "HeyGen finalizada com MP4 disponível");
        return new ProviderArtifacts(videoId, video, null, null, providerMetadata);
    }

    /** Cria a tarefa de vídeo na HeyGen. */
    private String submitRender(SalesVideoJob job, Map<String, Object> payload) {
        String path = properties.getProviders().getHeygen().getCreatePath();
        log.info("Chamando HeyGen para criar vídeo; jobId={} url={} request={}", job.id(), resolveBaseUrl() + path, payload);
        JsonNode response;
        try {
            response = authorized(webClient.post()
                            .uri(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "marketing-hub-sales-video-" + job.id())
                            .bodyValue(payload))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new VideoProviderException(
                    "PROVIDER_INVALID_REQUEST",
                    "HeyGen rejeitou criação do job %d com status %s: %s"
                            .formatted(job.id(), ex.getStatusCode(), ex.getResponseBodyAsString()),
                    ex);
        }
        log.info("Resposta HeyGen create; jobId={} response={}", job.id(), response);
        ensureSuccessfulResponse(response, "criação");
        String videoId = firstText(response, "/data/video_id", "/data/id", "/video_id", "/id");
        if (!StringUtils.hasText(videoId)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "HeyGen não retornou video_id para o job " + job.id());
        }
        return videoId;
    }

    /** Aguarda o vídeo HeyGen chegar em estado final com URL pública. */
    private JsonNode waitUntilCompleted(String videoId, ProgressCallback progressCallback) {
        VideoManagementProperties.HeyGen config = properties.getProviders().getHeygen();
        for (int attempt = 1; attempt <= config.getMaxPollAttempts(); attempt++) {
            String path = config.getStatusPathTemplate().replace("{videoId}", videoId);
            log.info("Consultando HeyGen; videoId={} url={}", videoId, resolveBaseUrl() + path);
            JsonNode status = authorized(webClient.get().uri(path))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            log.info("Resposta HeyGen status; videoId={} response={}", videoId, status);
            ensureSuccessfulResponse(status, "consulta");
            if (StringUtils.hasText(resolveVideoUrl(status))) {
                return status;
            }
            String failure = firstText(status, "/data/failure_message", "/data/failure_code", "/error/message");
            if (StringUtils.hasText(failure)) {
                throw new VideoProviderException("PROVIDER_RENDER_FAILED", "HeyGen falhou: " + failure);
            }
            int progress = Math.min(80, 30 + attempt);
            progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                    "HeyGen ainda processando (tentativa %d/%d)".formatted(attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão da HeyGen");
    }

    /** Monta payload oficial de criação de vídeo com avatar, voz, roteiro e motion prompt. */
    private Map<String, Object> buildPayload(SalesVideoJob job,
                                             SalesVideoProfile profile,
                                             SalesVideoScript script,
                                             JsonNode metadata) {
        VideoManagementProperties.HeyGen config = properties.getProviders().getHeygen();
        String avatarId = resolveRequired(metadata, "heygen_avatar_id", config.getAvatarId(), "VIDEO_PROVIDERS_HEYGEN_AVATAR_ID");
        String voiceId = resolveRequired(metadata, "heygen_voice_id", config.getVoiceId(), "VIDEO_PROVIDERS_HEYGEN_VOICE_ID");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "avatar");
        payload.put("avatar_id", avatarId);
        payload.put("title", trimToLength(nullToDefault(profile.title(), "Marketing Hub HeyGen video"), 120));
        payload.put("aspect_ratio", config.getAspectRatio());
        payload.put("output_format", config.getOutputFormat());
        payload.put("script", script.scriptText());
        payload.put("voice_id", voiceId);
        payload.put("voice_settings", voiceSettings(config, profile));
        payload.put("engine", Map.of("type", config.getEngineType()));
        if (shouldSendMotionPrompt(config, metadata)) {
            payload.put("motion_prompt", buildMotionPrompt(job, profile, script, metadata));
        }
        if (StringUtils.hasText(config.getBackgroundValue())) {
            payload.put("background", Map.of("type", "color", "value", config.getBackgroundValue()));
        }
        if (config.isCaptionEnabled()) {
            payload.put("caption", Map.of("file_format", "srt", "style", config.getCaptionStyle()));
        }
        return payload;
    }

    /** Evita enviar motion_prompt para Avatar IV quando a HeyGen não aceita esse campo. */
    private boolean shouldSendMotionPrompt(VideoManagementProperties.HeyGen config, JsonNode metadata) {
        if (metadata.path("heygen_motion_prompt_enabled").asBoolean(false)) {
            return true;
        }
        String engineType = Optional.ofNullable(config.getEngineType()).orElse("").toLowerCase(Locale.ROOT);
        return "avatar_v".equals(engineType);
    }

    /** Monta configurações de voz para português brasileiro com ritmo comercial. */
    private Map<String, Object> voiceSettings(VideoManagementProperties.HeyGen config, SalesVideoProfile profile) {
        Map<String, Object> voiceSettings = new LinkedHashMap<>();
        voiceSettings.put("speed", config.getVoiceSpeed());
        voiceSettings.put("pitch", config.getVoicePitch());
        voiceSettings.put("volume", config.getVoiceVolume());
        voiceSettings.put("locale", nullToDefault(profile.language(), "pt-BR"));
        return voiceSettings;
    }

    /** Monta direção de cena para evitar avatar sensualizado e manter promessa prática do MUSA. */
    private String buildMotionPrompt(SalesVideoJob job,
                                     SalesVideoProfile profile,
                                     SalesVideoScript script,
                                     JsonNode metadata) {
        String visualDirectives = visualProviderDirectives(metadata);
        return """
                Vertical mobile sales video for Método MUSA.
                Audience: Brazilian women seeking accessible elegance and practical confidence.
                Hook: %s
                CTA: %s
                Visual direction: %s
                The presenter must sound natural, warm, confident and practical. No sensualized pose, no body-focused framing, no provocative gaze, no luxury ostentation.
                Emphasize clarity, organization, small actions, relief and a realistic 7-day path.
                """.formatted(
                nullToDefault(script.hookText(), ""),
                nullToDefault(script.ctaText(), ""),
                visualDirectives).trim();
    }

    /** Extrai diretivas visuais específicas ou usa padrão anti-sensualização do MUSA. */
    private String visualProviderDirectives(JsonNode metadata) {
        String directives = metadata.path("visual_provider_directives").asText("");
        if (StringUtils.hasText(directives)) {
            return directives.trim();
        }
        return "Natural presenter, direct-to-camera, clear face, stable light, modest clothing, practical elegance, no sensualization.";
    }

    /** Baixa o MP4 final retornado pela HeyGen. */
    private ProviderFile downloadVideo(SalesVideoJob job, String videoUrl) {
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(URI.create(videoUrl))
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Download vazio do vídeo HeyGen");
        }
        MediaType contentType = response.getHeaders().getContentType();
        if (!isAcceptableMp4Download(contentType, content)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Download da HeyGen não retornou MP4 válido; contentType=%s bytes=%d"
                            .formatted(contentType, content.length));
        }
        return new ProviderFile("sales-video-" + job.id() + "-heygen.mp4",
                VIDEO_MP4,
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                content);
    }

    /** Consolida metadados de auditoria do render HeyGen sem expor chave de API. */
    private Map<String, Object> metadata(SalesVideoJob job,
                                         String videoId,
                                         Map<String, Object> request,
                                         JsonNode finalStatus) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "HEYGEN");
        metadata.put("provider_job_id", videoId);
        metadata.put("aspect_ratio", properties.getProviders().getHeygen().getAspectRatio());
        metadata.put("engine_type", properties.getProviders().getHeygen().getEngineType());
        metadata.put("request", request);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Verifica envelope de erro da API HeyGen. */
    private void ensureSuccessfulResponse(JsonNode response, String operation) {
        if (response == null || response.isNull()) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "HeyGen retornou resposta vazia em " + operation);
        }
        JsonNode error = response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "HeyGen retornou erro em %s: code=%s message=%s"
                            .formatted(operation, error.path("code").asText(), error.path("message").asText()));
        }
    }

    /** Extrai URL de vídeo pronta aceitando vídeo com legenda quando disponível. */
    private String resolveVideoUrl(JsonNode status) {
        String captioned = firstText(status, "/data/captioned_video_url");
        if (StringUtils.hasText(captioned)) {
            return captioned;
        }
        return firstText(status, "/data/video_url", "/video_url", "/url");
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

    /** Garante que o perfil possui roteiro aprovado para renderização. */
    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização HeyGen");
        }
        return script;
    }

    /** Bloqueia execução real sem chave HeyGen configurada. */
    private void requireApiKey() {
        if (!StringUtils.hasText(resolveApiKey())) {
            throw new VideoProviderException("PROVIDER_AUTH_ERROR", "HEYGEN_API_KEY não configurada para HeyGen");
        }
    }

    /** Aplica autenticação por X-Api-Key nas chamadas HeyGen. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        request.header("X-Api-Key", resolveApiKey());
        request.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    /** Resolve a chave HeyGen por valor direto ou arquivo de secret montado no container. */
    private String resolveApiKey() {
        VideoManagementProperties.HeyGen config = properties.getProviders().getHeygen();
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
            throw new UncheckedIOException("Não foi possível ler HEYGEN_API_KEY_FILE para HeyGen", ex);
        }
    }

    /** Lê metadados comerciais do job para montar request e auditoria. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (job == null || !StringUtils.hasText(job.metadataJson())) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (IOException ex) {
            throw new VideoProviderException("PROVIDER_INVALID_REQUEST",
                    "metadataJson inválido para job HeyGen " + job.id(), ex);
        }
    }

    /** Resolve valor obrigatório de metadata ou configuração do provider. */
    private String resolveRequired(JsonNode metadata, String metadataKey, String configuredValue, String environmentName) {
        String metadataValue = metadata.path(metadataKey).asText("");
        if (StringUtils.hasText(metadataValue)) {
            return metadataValue.trim();
        }
        if (StringUtils.hasText(configuredValue)) {
            return configuredValue.trim();
        }
        throw new VideoProviderException("PROVIDER_INVALID_REQUEST",
                "HeyGen exige " + metadataKey + " no metadataJson ou " + environmentName + " no executor");
    }

    /** Resolve base URL configurada para a API HeyGen. */
    private String resolveBaseUrl() {
        return properties.getProviders().getHeygen().getBaseUrl().toString();
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

    /** Aceita MP4 mesmo quando CDN/provider entrega o arquivo como octet-stream. */
    private boolean isAcceptableMp4Download(MediaType contentType, byte[] content) {
        if (!looksLikeMp4(content)) {
            return false;
        }
        if (contentType == null || VIDEO_MP4.isCompatibleWith(contentType)) {
            return true;
        }
        String normalized = contentType.toString().toLowerCase(Locale.ROOT);
        return normalized.contains("octet-stream");
    }

    /** Normaliza textos de provider e status. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /** Substitui valores vazios por padrão textual seguro para o prompt. */
    private String nullToDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /** Limita texto a tamanho aceito com folga pela API externa. */
    private String trimToLength(String value, int maxLength) {
        String text = nullToDefault(value, "");
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /** Aguarda entre tentativas de polling preservando interrupção da thread. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("Thread interrompida durante polling da HeyGen", ex);
        }
    }
}
