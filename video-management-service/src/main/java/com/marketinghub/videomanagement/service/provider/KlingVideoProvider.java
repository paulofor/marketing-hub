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

/** Responsabilidade: renderizar vídeos comerciais pelo provider Kling para jobs do Marketing Hub. */
@Component
@ConditionalOnProperty(prefix = "video.providers.kling", name = "enabled", havingValue = "true")
public class KlingVideoProvider implements VideoProvider {
    private static final Logger log = LoggerFactory.getLogger(KlingVideoProvider.class);
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 100 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final WebClient downloadWebClient;

    /** Inicializa o provider Kling com configuração, mapper JSON e clients HTTP. */
    public KlingVideoProvider(VideoManagementProperties properties,
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

    /** Verifica se o job de render pertence ao provider Kling. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        if (!StringUtils.hasText(providerName)) {
            return false;
        }
        return properties.getProviders().getKling().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Envia o prompt para Kling, aguarda conclusão, baixa o MP4 e devolve artefatos auditáveis. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        requireApiKey();
        SalesVideoScript script = ensureScript(profile);
        progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING, "Enviando prompt para Kling");

        Map<String, Object> payload = buildPayload(job, profile, script);
        boolean imageToVideo = isImageToVideoPayload(payload);
        String taskId = submitRender(job, payload, imageToVideo);
        progressCallback.onProgress(30, SalesVideoStatus.VIDEO_PROCESSING, "Kling aceitou o taskId: " + taskId);

        JsonNode finalStatus = waitUntilCompleted(taskId, imageToVideo, progressCallback);
        String videoUrl = resolveVideoUrl(finalStatus);
        if (!StringUtils.hasText(videoUrl)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Kling não retornou URL do vídeo gerado");
        }

        progressCallback.onProgress(85, SalesVideoStatus.VIDEO_PROCESSING, "Baixando MP4 gerado pelo Kling");
        ProviderFile video = downloadVideo(job, videoUrl);
        Map<String, Object> metadata = metadata(job, taskId, payload, finalStatus);
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Kling finalizado com MP4 disponível");
        return new ProviderArtifacts(taskId, video, null, null, metadata);
    }

    /** Cria a tarefa text-to-video ou image-to-video no Kling. */
    private String submitRender(SalesVideoJob job, Map<String, Object> payload, boolean imageToVideo) {
        String path = imageToVideo
                ? properties.getProviders().getKling().getImageCreatePath()
                : properties.getProviders().getKling().getCreatePath();
        log.info("Chamando Kling para criar vídeo; jobId={} url={} request={}", job.id(), resolveBaseUrl() + path, payload);
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
        log.info("Resposta Kling create; jobId={} response={}", job.id(), response);
        ensureSuccessfulResponse(response, "criação");
        String taskId = firstText(response,
                "/data/task_id",
                "/data/taskId",
                "/task_id",
                "/taskId",
                "/id");
        if (!StringUtils.hasText(taskId)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Kling não retornou task_id para o job " + job.id());
        }
        return taskId;
    }

    /** Aguarda a tarefa Kling chegar em sucesso ou falha objetiva. */
    private JsonNode waitUntilCompleted(String taskId, boolean imageToVideo, ProgressCallback progressCallback) {
        VideoManagementProperties.Kling config = properties.getProviders().getKling();
        for (int attempt = 1; attempt <= config.getMaxPollAttempts(); attempt++) {
            String statusTemplate = imageToVideo ? config.getImageStatusPathTemplate() : config.getStatusPathTemplate();
            String path = statusTemplate.replace("{taskId}", taskId);
            log.info("Consultando Kling; taskId={} url={}", taskId, resolveBaseUrl() + path);
            JsonNode status;
            try {
                status = authorized(webClient.get().uri(path))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
            } catch (WebClientResponseException ex) {
                throw providerHttpError(null, "consulta", path, ex);
            }
            log.info("Resposta Kling status; taskId={} response={}", taskId, status);
            ensureSuccessfulResponse(status, "consulta");
            String taskStatus = normalize(firstText(status,
                    "/data/task_status",
                    "/data/taskStatus",
                    "/data/status",
                    "/task_status",
                    "/status"));
            if (isSuccess(taskStatus)) {
                return status;
            }
            if (isFailure(taskStatus)) {
                throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                        "Kling falhou: " + readFailure(status));
            }
            int progress = Math.min(80, 30 + attempt);
            progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                    "Kling ainda processando (tentativa %d/%d)".formatted(attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão do Kling");
    }

    /** Monta payload text-to-video ou image-to-video com prompt comercial e parâmetros do provider. */
    private Map<String, Object> buildPayload(SalesVideoJob job, SalesVideoProfile profile, SalesVideoScript script) {
        VideoManagementProperties.Kling config = properties.getProviders().getKling();
        JsonNode metadata = readMetadata(job);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model_name", config.getModel());
        payload.put("prompt", buildPrompt(job, profile, script));
        payload.put("negative_prompt", config.getNegativePrompt());
        payload.put("aspect_ratio", config.getAspectRatio());
        payload.put("mode", config.getMode());
        payload.put("duration", resolveDuration(metadata));
        String sourceImageUrl = resolveSourceImageUrl(metadata);
        if (StringUtils.hasText(sourceImageUrl)) {
            payload.put("image", sourceImageUrl);
        }
        return payload;
    }

    /** Identifica se o payload deve usar o endpoint image-to-video. */
    private boolean isImageToVideoPayload(Map<String, Object> payload) {
        Object image = payload.get("image");
        return image instanceof String imageUrl && StringUtils.hasText(imageUrl);
    }

    /** Resolve URL de imagem aprovada a partir do metadata estruturado do job. */
    private String resolveSourceImageUrl(JsonNode metadata) {
        return firstText(metadata,
                "/image_to_video/source_image_url",
                "/image_to_video/reference_image_url",
                "/characterImageReferenceUrl",
                "/promptImage");
    }

    /** Monta prompt orientado ao PDE MUSA e às diretivas visuais do job. */
    private String buildPrompt(SalesVideoJob job, SalesVideoProfile profile, SalesVideoScript script) {
        JsonNode metadata = readMetadata(job);
        String visualDirectives = visualProviderDirectives(metadata);
        String scenes = "SCENE_BY_SCENE_MONTAGE".equalsIgnoreCase(
                        metadata.path("generation_strategy").asText(""))
                && metadata.path("scene").isObject()
                ? metadata.path("scene").toString()
                : metadata.path("assembly_plan").path("scenes").isMissingNode()
                        ? "Recognizable pain, plausible mechanism, personal value and CTA."
                        : metadata.path("assembly_plan").path("scenes").toString();
        return """
                Vertical 9:16 short-form sales video for Método MUSA - Presença Elegante em 7 Dias.
                Language: %s.
                Title: %s.
                Audience: Brazilian urban women who want accessible sophistication, less effort and more intentional presence.
                Product promise: build a more elegant, coherent and possible presence in 7 days with practical micro-actions using what she already owns.
                Approved hook: %s.
                Approved script context: %s.
                Approved CTA: %s.
                Scene plan: %s.
                Provider-specific visual directives: %s.
                Must feel practical, respectful and relatable. Show wardrobe decisions, removing visual noise, choosing one discreet signal piece, adjusting color, finish and posture.
                Do not sensualize the woman. No seductive posing, no body-focused framing, no provocative gaze, no luxury ostentation, no embedded text and no logo.
                """.formatted(
                nullToDefault(profile.language(), "pt-BR"),
                nullToDefault(profile.title(), "Sales video"),
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
        return "Sharp image, stable exposure, constant natural daylight, no haze, no blur and no sensualized pose.";
    }

    /** Baixa o MP4 final retornado pelo Kling. */
    private ProviderFile downloadVideo(SalesVideoJob job, String videoUrl) {
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(URI.create(videoUrl))
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Download vazio do vídeo Kling");
        }
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null || !VIDEO_MP4.isCompatibleWith(contentType) || !looksLikeMp4(content)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Download do Kling não retornou MP4 válido; contentType=%s bytes=%d"
                            .formatted(contentType, content.length));
        }
        return new ProviderFile("sales-video-" + job.id() + "-kling.mp4",
                VIDEO_MP4,
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                content);
    }

    /** Consolida metadados de auditoria do render Kling. */
    private Map<String, Object> metadata(SalesVideoJob job,
                                         String taskId,
                                         Map<String, Object> request,
                                         JsonNode finalStatus) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "KLING_3_0");
        metadata.put("provider_job_id", taskId);
        metadata.put("model", properties.getProviders().getKling().getModel());
        metadata.put("aspect_ratio", properties.getProviders().getKling().getAspectRatio());
        metadata.put("mode", properties.getProviders().getKling().getMode());
        metadata.put("duration_seconds", parseDurationSeconds(request.get("duration").toString()));
        metadata.put("modality", isImageToVideoPayload(request) ? "image_to_video" : "text_to_video");
        metadata.put("cost_usd", estimateCostUsd(request));
        metadata.put("pricing_source", "Kling API pricing varies by model, mode, resolution and duration");
        metadata.put("request", request);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Calcula custo aproximado conservador para teste Kling standard. */
    private BigDecimal estimateCostUsd(Map<String, Object> request) {
        int duration = parseDurationSeconds(String.valueOf(request.get("duration")));
        BigDecimal fiveSecondCost = "pro".equalsIgnoreCase(properties.getProviders().getKling().getMode())
                ? new BigDecimal("0.33")
                : new BigDecimal("0.20");
        return fiveSecondCost.multiply(BigDecimal.valueOf(Math.max(1, duration / 5L)));
    }

    /** Resolve cinco ou dez segundos conforme o contrato auditável da cena solicitado pelo Estúdio. */
    private String resolveDuration(JsonNode metadata) {
        int requested = metadata.path("provider_strategy").path("expected_clip_duration_seconds")
                .asInt(metadata.path("scene").path("duration_seconds").asInt(0));
        if (requested >= 10) {
            return "10";
        }
        return properties.getProviders().getKling().getDuration();
    }

    /** Verifica envelope de sucesso quando a API retorna code/message padronizados. */
    private void ensureSuccessfulResponse(JsonNode response, String operation) {
        if (response == null || response.isNull()) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Kling retornou resposta vazia em " + operation);
        }
        if (response.has("code") && response.path("code").asInt(0) != 0) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Kling retornou erro em %s: code=%s message=%s"
                            .formatted(operation, response.path("code").asText(), response.path("message").asText()));
        }
    }

    /** Converte erro HTTP do provider em falha auditável com corpo sanitizado. */
    private VideoProviderException providerHttpError(SalesVideoJob job,
                                                     String operation,
                                                     String path,
                                                     WebClientResponseException ex) {
        String body = sanitizeProviderBody(ex.getResponseBodyAsString());
        log.warn("Kling retornou erro HTTP; jobId={} operation={} status={} url={} responseBody={}",
                job == null ? null : job.id(),
                operation,
                ex.getStatusCode().value(),
                resolveBaseUrl() + path,
                body,
                ex);
        String code = ex.getStatusCode().value() == 429 ? "PROVIDER_RATE_LIMIT" : "PROVIDER_RENDER_FAILED";
        return new VideoProviderException(code,
                "Kling retornou HTTP %d em %s: %s"
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

    /** Extrai URL de vídeo aceitando formatos atuais e variações comuns do Kling. */
    private String resolveVideoUrl(JsonNode status) {
        String direct = firstText(status,
                "/data/task_result/videos/0/url",
                "/data/taskResult/videos/0/url",
                "/data/videos/0/url",
                "/data/video_url",
                "/data/videoUrl",
                "/video_url",
                "/videoUrl",
                "/url");
        return StringUtils.hasText(direct) ? direct : null;
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

    /** Identifica status de sucesso do Kling. */
    private boolean isSuccess(String status) {
        return "SUCCEED".equals(status) || "SUCCESS".equals(status) || "COMPLETED".equals(status);
    }

    /** Identifica status de falha do Kling. */
    private boolean isFailure(String status) {
        return "FAILED".equals(status) || "FAIL".equals(status) || "CANCELED".equals(status) || "CANCELLED".equals(status);
    }

    /** Lê motivo de falha retornado pelo Kling. */
    private String readFailure(JsonNode status) {
        String message = firstText(status,
                "/data/task_status_msg",
                "/data/taskStatusMsg",
                "/data/message",
                "/message",
                "/error/message");
        return StringUtils.hasText(message) ? message : status.toString();
    }

    /** Garante que o perfil possui roteiro aprovado para renderização. */
    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização Kling");
        }
        return script;
    }

    /** Bloqueia execução real sem chave Kling configurada. */
    private void requireApiKey() {
        if (!StringUtils.hasText(resolveApiKey())) {
            throw new VideoProviderException("PROVIDER_AUTH_ERROR", "KLING_API_KEY não configurada para Kling");
        }
    }

    /** Aplica autenticação Bearer nas chamadas Kling. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveApiKey());
        request.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    /** Resolve a chave Kling por valor direto ou arquivo de secret montado no container. */
    private String resolveApiKey() {
        VideoManagementProperties.Kling config = properties.getProviders().getKling();
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
            throw new UncheckedIOException("Não foi possível ler KLING_API_KEY_FILE para Kling", ex);
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
                    "metadataJson inválido para job Kling " + job.id(), ex);
        }
    }

    /** Resolve base URL configurada para a API Kling. */
    private String resolveBaseUrl() {
        return properties.getProviders().getKling().getBaseUrl().toString();
    }

    /** Converte duração textual para segundos. */
    private int parseDurationSeconds(String duration) {
        String normalized = normalize(duration).replace("S", "");
        return normalized.matches("\\d+") ? Integer.parseInt(normalized) : 5;
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
            throw new VideoProviderException("Thread interrompida durante polling do Kling", ex);
        }
    }
}
