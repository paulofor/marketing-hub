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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
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
    private static final String COMMERCIAL_PROMPT_PATH = "prompts/sales-video/kling-commercial-v1.md";
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 100 * 1024 * 1024;
    static final int MAX_PROMPT_CHARACTERS = 2500;
    static final int MAX_PROMPT_UTF8_BYTES = 2500;

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
        JsonNode jobMetadata = readMetadata(job);
        int sceneCount = resolveSceneCount(jobMetadata);
        List<ProviderFile> scenes = new ArrayList<>();
        List<Map<String, Object>> sceneMetadata = new ArrayList<>();
        List<String> taskIds = new ArrayList<>();
        Map<String, Object> lastPayload = null;
        JsonNode lastStatus = null;
        for (int scene = 1; scene <= sceneCount; scene++) {
            progressCallback.onProgress(5 + ((scene - 1) * 75 / sceneCount),
                    SalesVideoStatus.VIDEO_PROCESSING,
                    "Enviando cena %d/%d para Kling".formatted(scene, sceneCount));
            Map<String, Object> payload = buildPayload(job, profile, script, scene, sceneCount);
            boolean imageToVideo = isImageToVideoPayload(payload);
            String taskId = submitRender(job, payload, imageToVideo);
            int durationSeconds = parseDurationSeconds(String.valueOf(payload.get("duration")));
            int estimatedCredits = estimateCredits(durationSeconds);
            taskIds.add(taskId);
            progressCallback.onProgress(10 + (scene * 65 / sceneCount), SalesVideoStatus.VIDEO_PROCESSING,
                    "Kling aceitou cena %d/%d; taskId=%s".formatted(scene, sceneCount, taskId),
                    providerTaskDetails(taskId, scene, sceneCount, durationSeconds, estimatedCredits));

            JsonNode finalStatus = waitUntilCompleted(taskId, imageToVideo, progressCallback);
            progressCallback.onProgress(10 + (scene * 70 / sceneCount), SalesVideoStatus.VIDEO_PROCESSING,
                    "Kling liquidou cena %d/%d; taskId=%s".formatted(scene, sceneCount, taskId),
                    providerTaskSettlementDetails(taskId, scene, sceneCount, durationSeconds, estimatedCredits));
            String videoUrl = resolveVideoUrl(finalStatus);
            if (!StringUtils.hasText(videoUrl)) {
                throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                        "Kling não retornou URL da cena " + scene);
            }
            scenes.add(downloadVideo(job, videoUrl));
            sceneMetadata.add(sceneMetadata(scene, taskId, payload, finalStatus));
            lastPayload = payload;
            lastStatus = finalStatus;
        }

        progressCallback.onProgress(86, SalesVideoStatus.VIDEO_PROCESSING, "Montando vídeo final Kling");
        ProviderFile video = scenes.size() == 1 ? scenes.getFirst() : assembleScenes(job, scenes);
        String providerJobId = String.join(",", taskIds);
        Map<String, Object> metadata = metadata(
                job, providerJobId, lastPayload, lastStatus, sceneCount, sceneMetadata);
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING,
                "Kling finalizado com MP4 integral disponível");
        return new ProviderArtifacts(providerJobId, video, null, null, metadata);
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
    private Map<String, Object> buildPayload(SalesVideoJob job,
                                             SalesVideoProfile profile,
                                             SalesVideoScript script,
                                             int scene,
                                             int sceneCount) {
        VideoManagementProperties.Kling config = properties.getProviders().getKling();
        JsonNode metadata = readMetadata(job);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model_name", config.getModel());
        payload.put("prompt", buildPrompt(job, profile, script, scene, sceneCount));
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

    /** Monta o prompt versionado a partir do produto, da pessoa e do roteiro persistidos. */
    private String buildPrompt(SalesVideoJob job,
                               SalesVideoProfile profile,
                               SalesVideoScript script,
                               int scene,
                               int sceneCount) {
        JsonNode metadata = readMetadata(job);
        String visualDirectives = visualProviderDirectives(metadata);
        String scenes = "SCENE_BY_SCENE_MONTAGE".equalsIgnoreCase(
                        metadata.path("generation_strategy").asText(""))
                && metadata.path("scene").isObject()
                ? metadata.path("scene").toString()
                : plannedSceneBrief(metadata, scene, sceneCount);
        try {
            String prompt = new ClassPathResource(COMMERCIAL_PROMPT_PATH)
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{SCENE_NUMBER}}", String.valueOf(scene))
                    .replace("{{SCENE_COUNT}}", String.valueOf(sceneCount))
                    .replace("{{SCENE_BRIEF}}", compactPromptField(scenes, 850))
                    .replace("{{LANGUAGE}}", compactPromptField(nullToDefault(profile.language(), "pt-BR"), 12))
                    .replace("{{TITLE}}", compactPromptField(nullToDefault(profile.title(), "Vídeo comercial"), 60))
                    .replace("{{PERSONA_NAME}}", compactPromptField(
                            nullToDefault(profile.personaName(), "Pessoa do público-alvo"), 60))
                    .replace("{{PERSONA_STYLE}}", compactPromptField(
                            nullToDefault(profile.personaStyle(), "Contexto real do público-alvo"), 100))
                    .replace("{{APPROVED_HOOK}}", compactPromptField(nullToDefault(script.hookText(), ""), 100))
                    .replace("{{APPROVED_SCRIPT}}", compactPromptField(script.scriptText(), 140))
                    .replace("{{APPROVED_CTA}}", compactPromptField(nullToDefault(script.ctaText(), ""), 80))
                    .replace("{{VISUAL_DIRECTIVES}}", compactPromptField(visualDirectives, 220));
            int promptBytes = prompt.getBytes(StandardCharsets.UTF_8).length;
            if (prompt.length() > MAX_PROMPT_CHARACTERS || promptBytes > MAX_PROMPT_UTF8_BYTES) {
                throw new VideoProviderException(
                        "PROVIDER_PROMPT_INVALID",
                        "Prompt Kling excedeu o contrato local de %d caracteres/%d bytes"
                                .formatted(MAX_PROMPT_CHARACTERS, MAX_PROMPT_UTF8_BYTES));
            }
            return prompt;
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível carregar prompt comercial Kling", ex);
        }
    }

    /** Normaliza e limita um campo do prompt sem remover as regras de segurança do template. */
    private String compactPromptField(String value, int maxCharacters) {
        String normalized = nullToDefault(value, "").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxCharacters - 1)).trim() + "…";
    }

    /** Seleciona somente os cortes ou cenas do clipe atual para evitar repetição narrativa. */
    private String plannedSceneBrief(JsonNode metadata, int scene, int sceneCount) {
        JsonNode cuts = metadata.path("cut_plan");
        if (cuts.isArray() && !cuts.isEmpty()) {
            int start = (scene - 1) * cuts.size() / sceneCount;
            int end = Math.max(start + 1, scene * cuts.size() / sceneCount);
            List<String> selected = new ArrayList<>();
            for (int index = start; index < Math.min(end, cuts.size()); index++) {
                JsonNode cut = cuts.get(index);
                selected.add("Corte %d (%ds, %s): %s Continuidade: %s".formatted(
                        cut.path("order").asInt(index + 1),
                        cut.path("duration_seconds").asInt(3),
                        cut.path("role").asText("MECANISMO"),
                        compactPromptField(cut.path("visual_objective").asText("ação visual única"), 180),
                        compactPromptField(
                                cut.path("continuity_anchor")
                                        .asText("mesma personagem, figurino, ambiente e luz"),
                                45)));
            }
            return String.join(" ", selected);
        }
        JsonNode assemblyScenes = metadata.path("assembly_plan").path("scenes");
        if (!assemblyScenes.isArray() || assemblyScenes.isEmpty()) {
            return "Recognizable pain, plausible mechanism, personal value and CTA.";
        }
        int start = (scene - 1) * assemblyScenes.size() / sceneCount;
        int end = Math.max(start + 1, scene * assemblyScenes.size() / sceneCount);
        List<String> selected = new ArrayList<>();
        for (int index = start; index < Math.min(end, assemblyScenes.size()); index++) {
            JsonNode item = assemblyScenes.get(index);
            selected.add("%s — %s: %s; ação: %s; câmera: %s".formatted(
                    item.path("role").asText("CENA"),
                    compactPromptField(item.path("title").asText(""), 50),
                    compactPromptField(item.path("message").asText(""), 100),
                    compactPromptField(item.path("action").asText(""), 100),
                    compactPromptField(item.path("camera").asText(""), 50)));
        }
        return String.join(" ", selected);
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
                                         JsonNode finalStatus,
                                         int sceneCount,
                                         List<Map<String, Object>> scenes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "KLING_3_0");
        metadata.put("provider_job_id", taskId);
        metadata.put("model", properties.getProviders().getKling().getModel());
        metadata.put("aspect_ratio", properties.getProviders().getKling().getAspectRatio());
        metadata.put("mode", properties.getProviders().getKling().getMode());
        int clipDurationSeconds = parseDurationSeconds(request.get("duration").toString());
        metadata.put("duration_seconds", clipDurationSeconds * sceneCount);
        metadata.put("clip_duration_seconds", clipDurationSeconds);
        metadata.put("scene_count", sceneCount);
        metadata.put("assembled_locally", sceneCount > 1);
        metadata.put("modality", isImageToVideoPayload(request) ? "image_to_video" : "text_to_video");
        metadata.put("cost_usd", estimateCostUsd(request).multiply(BigDecimal.valueOf(sceneCount)));
        metadata.put("cost_scope", "ALL_SCENES");
        metadata.put("pricing_source", "Kling API pricing varies by model, mode, resolution and duration");
        metadata.put("request", request);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        metadata.put("scenes", scenes);
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Resolve a quantidade de clipes paga e aprovada no contrato do Estúdio. */
    int resolveSceneCount(JsonNode metadata) {
        return Math.max(1, metadata.path("sceneCount").asInt(1));
    }

    /** Consolida o request e o retorno bruto de cada cena sem perder a correlação do provider. */
    private Map<String, Object> sceneMetadata(int scene,
                                              String taskId,
                                              Map<String, Object> request,
                                              JsonNode finalStatus) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scene_number", scene);
        metadata.put("provider_task_id", taskId);
        metadata.put("request", request);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        return metadata;
    }

    /** Concatena clipes Kling homogêneos para entregar a duração integral aprovada por Plutus. */
    private ProviderFile assembleScenes(SalesVideoJob job, List<ProviderFile> scenes) {
        List<Path> temporaryFiles = new ArrayList<>();
        try {
            Path manifest = Files.createTempFile("kling-scenes-" + job.id(), ".txt");
            temporaryFiles.add(manifest);
            StringBuilder entries = new StringBuilder();
            for (ProviderFile scene : scenes) {
                Path file = Files.createTempFile("kling-scene-" + job.id(), ".mp4");
                Files.write(file, scene.content());
                temporaryFiles.add(file);
                entries.append("file '").append(file.toAbsolutePath()).append("'\n");
            }
            Files.writeString(manifest, entries);
            Path output = Files.createTempFile("kling-montage-" + job.id(), ".mp4");
            temporaryFiles.add(output);
            Process process = new ProcessBuilder(
                    properties.getProviders().getPostProduction().getFfmpegPath(),
                    "-y", "-f", "concat", "-safe", "0", "-i", manifest.toString(),
                    "-c", "copy", "-movflags", "+faststart", output.toString())
                    .redirectErrorStream(true)
                    .start();
            byte[] processOutput = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED",
                        "ffmpeg falhou ao montar cenas Kling; exitCode=%d output=%s"
                                .formatted(exitCode, new String(processOutput, java.nio.charset.StandardCharsets.UTF_8)));
            }
            return new ProviderFile("sales-video-" + job.id() + "-kling.mp4",
                    VIDEO_MP4, AssetType.VIDEO, ProviderAssetRole.VIDEO, Files.readAllBytes(output));
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Falha ao montar cenas Kling", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Montagem Kling interrompida", ex);
        } finally {
            temporaryFiles.forEach(this::deleteIfExists);
        }
    }

    /** Remove somente arquivos temporários criados pela montagem deste job. */
    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Não foi possível remover arquivo temporário Kling; path={}", path, ex);
        }
    }

    /** Converte a estimativa conservadora do Kling em créditos de um centavo. */
    private int estimateCredits(int durationSeconds) {
        return estimateCostUsd(Map.of("duration", String.valueOf(durationSeconds)))
                .movePointRight(2)
                .intValueExact();
    }

    /** Serializa a reserva financeira de uma cena aceita pelo provider. */
    private String providerTaskDetails(String taskId,
                                       int scene,
                                       int sceneCount,
                                       int durationSeconds,
                                       int estimatedCredits) {
        return writeProviderTaskDetails(
                "PROVIDER_TASK_ACCEPTED", taskId, scene, sceneCount, durationSeconds,
                estimatedCredits, "ACCEPTED", "ESTIMATED_AT_ACCEPTANCE");
    }

    /** Serializa a liquidação conservadora de uma cena concluída pelo provider. */
    private String providerTaskSettlementDetails(String taskId,
                                                  int scene,
                                                  int sceneCount,
                                                  int durationSeconds,
                                                  int billedCredits) {
        return writeProviderTaskDetails(
                "PROVIDER_TASK_SETTLED", taskId, scene, sceneCount, durationSeconds,
                billedCredits, "SUCCEEDED", "PROVIDER_COMPLETION_WITH_CATALOG_PRICE");
    }

    /** Monta o evento financeiro idempotente consumido pelo ledger central. */
    private String writeProviderTaskDetails(String eventType,
                                            String taskId,
                                            int scene,
                                            int sceneCount,
                                            int durationSeconds,
                                            int credits,
                                            String settlementStatus,
                                            String settlementBasis) {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("eventType", eventType);
            details.put("provider", "KLING");
            details.put("providerTaskId", taskId);
            details.put("model", properties.getProviders().getKling().getModel());
            details.put("sceneNumber", scene);
            details.put("plannedSceneCount", sceneCount);
            details.put("durationSeconds", durationSeconds);
            if ("PROVIDER_TASK_ACCEPTED".equals(eventType)) {
                details.put("estimatedCredits", credits);
                details.put("estimatedCostUsd", BigDecimal.valueOf(credits).movePointLeft(2));
            } else {
                details.put("billedCredits", credits);
                details.put("billedCostUsd", BigDecimal.valueOf(credits).movePointLeft(2));
                details.put("settlementStatus", settlementStatus);
                details.put("settlementBasis", settlementBasis);
                details.put("billingEvidence", "Kling concluiu a task; custo conciliado pela tabela do adapter.");
            }
            return objectMapper.writeValueAsString(details);
        } catch (IOException ex) {
            log.error("Falha ao serializar consumo Kling; taskId={}", taskId, ex);
            throw new VideoProviderException("PROVIDER_AUDIT_FAILED", "Falha ao auditar consumo Kling", ex);
        }
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
