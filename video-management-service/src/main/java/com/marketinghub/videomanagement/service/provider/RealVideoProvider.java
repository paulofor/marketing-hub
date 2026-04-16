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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter de provider real com fluxo request -> polling -> download.
 */
@Component
@ConditionalOnProperty(prefix = "video.providers.real", name = "enabled", havingValue = "true")
public class RealVideoProvider implements VideoProvider {
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final MediaType IMAGE_PNG = MediaType.IMAGE_PNG;
    private static final MediaType TEXT_VTT = MediaType.valueOf("text/vtt");

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public RealVideoProvider(VideoManagementProperties properties,
                             ObjectMapper objectMapper,
                             WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl(resolveBaseUrl(properties)).build();
    }

    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        if (!StringUtils.hasText(providerName)) {
            return false;
        }
        return properties.getProviders().getReal().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        SalesVideoScript script = ensureScript(profile);
        progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING, "Enviando render para provider real");

        String providerJobId = submitRender(job, profile, script);
        progressCallback.onProgress(35, SalesVideoStatus.VIDEO_PROCESSING,
                "Render aceito pelo provider: " + providerJobId);

        JsonNode finalStatus = waitUntilFinished(providerJobId, progressCallback);
        String status = readText(finalStatus, "status");
        if ("FAILED".equals(status)) {
            throw new VideoProviderException(resolveFailureCode(finalStatus), readText(finalStatus, "error_message"));
        }
        if ("EXPIRED".equals(status)) {
            throw new VideoProviderException("PROVIDER_ASSET_EXPIRED", "Provider marcou o output como expirado");
        }

        progressCallback.onProgress(80, SalesVideoStatus.VIDEO_PROCESSING, "Baixando assets finais do provider");
        ProviderFile video = downloadRequiredFile(finalStatus, "video_url", VIDEO_MP4,
                AssetType.VIDEO, ProviderAssetRole.VIDEO, "sales-video-" + job.id() + ".mp4");
        ProviderFile poster = downloadOptionalFile(finalStatus, "poster_url", IMAGE_PNG,
                AssetType.IMAGE, ProviderAssetRole.POSTER, "sales-video-" + job.id() + "-poster.png");
        ProviderFile caption = downloadOptionalFile(finalStatus, "captions_url", TEXT_VTT,
                AssetType.CAPTION, ProviderAssetRole.CAPTION, "sales-video-" + job.id() + ".vtt");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", normalize(job.providerName()));
        metadata.put("provider_job_id", providerJobId);
        metadata.put("provider_status", status);
        metadata.put("polled_at", Instant.now().toString());
        JsonNode providerMetadata = finalStatus.path("metadata");
        if (!providerMetadata.isMissingNode() && !providerMetadata.isNull()) {
            metadata.put("provider_metadata", objectMapper.convertValue(providerMetadata, Map.class));
        }

        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Assets reais finalizados");
        return new ProviderArtifacts(providerJobId, video, poster, caption, metadata);
    }

    private String submitRender(SalesVideoJob job,
                                SalesVideoProfile profile,
                                SalesVideoScript script) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("job_id", job.id());
        payload.put("profile_id", profile.id());
        payload.put("title", profile.title());
        payload.put("language", profile.language());
        payload.put("persona_name", profile.personaName());
        payload.put("voice_style", profile.voiceStyle());
        payload.put("target_duration_seconds", profile.targetDurationSeconds());
        payload.put("script_text", script.scriptText());
        payload.put("hook_text", script.hookText());
        payload.put("cta_text", script.ctaText());
        payload.put("provider", normalize(job.providerName()));

        JsonNode response = authorized(webClient.post()
                        .uri(properties.getProviders().getReal().getCreatePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String providerJobId = readText(response, "provider_job_id");
        if (!StringUtils.hasText(providerJobId)) {
            providerJobId = readText(response, "id");
        }
        if (!StringUtils.hasText(providerJobId)) {
            throw new VideoProviderException("Resposta do provider sem provider_job_id");
        }
        return providerJobId;
    }

    private JsonNode waitUntilFinished(String providerJobId,
                                       ProgressCallback progressCallback) {
        VideoManagementProperties.Real config = properties.getProviders().getReal();
        for (int attempt = 1; attempt <= config.getMaxPollAttempts(); attempt++) {
            JsonNode statusResponse = fetchStatus(providerJobId);
            String status = normalize(readText(statusResponse, "status"));
            Integer percent = readInt(statusResponse, "progress_percent");
            if ("COMPLETED".equals(status)) {
                return statusResponse;
            }
            if ("FAILED".equals(status) || "EXPIRED".equals(status)) {
                return statusResponse;
            }
            int normalizedProgress = Math.max(40, Math.min(percent == null ? 0 : percent, 90));
            progressCallback.onProgress(normalizedProgress, SalesVideoStatus.VIDEO_PROCESSING,
                    "Provider em processamento (tentativa %d/%d)".formatted(attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão do provider real");
    }

    private JsonNode fetchStatus(String providerJobId) {
        String pathTemplate = properties.getProviders().getReal().getStatusPathTemplate();
        return authorized(webClient.get().uri(pathTemplate, providerJobId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private ProviderFile downloadRequiredFile(JsonNode payload,
                                              String field,
                                              MediaType mediaType,
                                              AssetType assetType,
                                              ProviderAssetRole role,
                                              String filename) {
        ProviderFile file = downloadOptionalFile(payload, field, mediaType, assetType, role, filename);
        if (file == null) {
            throw new VideoProviderException("Provider não retornou URL obrigatória: " + field);
        }
        return file;
    }

    private ProviderFile downloadOptionalFile(JsonNode payload,
                                              String field,
                                              MediaType mediaType,
                                              AssetType assetType,
                                              ProviderAssetRole role,
                                              String filename) {
        String url = readText(payload, field);
        if (!StringUtils.hasText(url)) {
            return null;
        }
        byte[] content = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("Download vazio para campo " + field);
        }
        return new ProviderFile(filename, mediaType, assetType, role, content);
    }

    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização");
        }
        return script;
    }

    private String resolveFailureCode(JsonNode finalStatus) {
        String code = readText(finalStatus, "error_code");
        return StringUtils.hasText(code) ? code : "PROVIDER_RENDER_FAILED";
    }

    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        String authToken = properties.getProviders().getReal().getAuthToken();
        if (StringUtils.hasText(authToken)) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
        }
        return request;
    }

    private String resolveBaseUrl(VideoManagementProperties properties) {
        if (properties.getProviders().getReal().getBaseUrl() == null) {
            throw new IllegalStateException("video.providers.real.base-url precisa ser configurado quando provider real estiver habilitado");
        }
        return properties.getProviders().getReal().getBaseUrl().toString();
    }

    private String readText(JsonNode node,
                            String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText(null);
    }

    private Integer readInt(JsonNode node,
                            String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asInt();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("Thread interrompida durante polling do provider", ex);
        }
    }
}
