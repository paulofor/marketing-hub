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
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

/** Adapter direto para renderizar vídeos comerciais usando VEO via Gemini API. */
@Component
@ConditionalOnProperty(prefix = "video.providers.veo", name = "enabled", havingValue = "true")
public class VeoVideoProvider implements VideoProvider {
    private static final Logger log = LoggerFactory.getLogger(VeoVideoProvider.class);
    private static final String COMMERCIAL_PROMPT_PATH = "prompts/sales-video/veo-commercial-v2.md";
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final int MAX_SCENES_PER_JOB = 3;
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 25 * 1024 * 1024;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final WebClient downloadWebClient;

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
        this.downloadWebClient = webClientBuilder.clone()
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

    /** Envia uma ou até três cenas para VEO, concilia cada operação e monta o MP4 integral. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        requireApiKey();
        SalesVideoScript script = ensureScript(profile);
        JsonNode jobMetadata = readMetadata(job);
        int sceneCount = resolveSceneCount(jobMetadata);
        BigDecimal estimatedCost = estimateCostUsd(
                properties.getProviders().getVeo().getModel(),
                properties.getProviders().getVeo().getDurationSeconds(),
                properties.getProviders().getVeo().getResolution()).multiply(BigDecimal.valueOf(sceneCount));
        ensureBudget(jobMetadata, estimatedCost);
        InputImage sourceImage = loadSourceImage(jobMetadata);
        List<ProviderFile> clips = new ArrayList<>();
        List<String> operationNames = new ArrayList<>();
        List<Map<String, Object>> scenes = new ArrayList<>();

        for (int scene = 1; scene <= sceneCount; scene++) {
            progressCallback.onProgress(5 + ((scene - 1) * 70 / sceneCount),
                    SalesVideoStatus.VIDEO_PROCESSING,
                    "Enviando cena %d/%d para VEO".formatted(scene, sceneCount));
            String operationName = submitRender(job, profile, script, scene, sceneCount, sourceImage);
            operationNames.add(operationName);
            BigDecimal sceneCost = estimatedCost.divide(BigDecimal.valueOf(sceneCount));
            progressCallback.onProgress(10 + (scene * 55 / sceneCount), SalesVideoStatus.VIDEO_PROCESSING,
                    "VEO aceitou cena %d/%d: %s".formatted(scene, sceneCount, operationName),
                    providerTaskDetails("PROVIDER_TASK_ACCEPTED", operationName, scene, sceneCount, sceneCost));

            JsonNode finalStatus = waitUntilDone(operationName, scene, sceneCount, progressCallback);
            ensureSuccessfulStatus(finalStatus);
            progressCallback.onProgress(15 + (scene * 60 / sceneCount), SalesVideoStatus.VIDEO_PROCESSING,
                    "VEO liquidou cena %d/%d: %s".formatted(scene, sceneCount, operationName),
                    providerTaskDetails("PROVIDER_TASK_SETTLED", operationName, scene, sceneCount, sceneCost));
            String videoUri = resolveVideoUri(finalStatus);
            clips.add(downloadVideo(job, videoUri, scene));
            scenes.add(sceneMetadata(scene, operationName, finalStatus));
        }

        progressCallback.onProgress(86, SalesVideoStatus.VIDEO_PROCESSING, "Montando vídeo final VEO");
        ProviderFile video = clips.size() == 1 ? clips.getFirst() : assembleScenes(job, clips);
        String providerJobId = String.join(",", operationNames);
        Map<String, Object> metadata = metadata(job, providerJobId, sceneCount, estimatedCost, scenes, sourceImage);
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "VEO finalizado com MP4 integral disponível");
        return new ProviderArtifacts(providerJobId, video, null, null, metadata);
    }

    /** Cria a operação long-running no endpoint predictLongRunning do VEO. */
    private String submitRender(SalesVideoJob job,
                                SalesVideoProfile profile,
                                SalesVideoScript script,
                                int scene,
                                int sceneCount,
                                InputImage sourceImage) {
        VideoManagementProperties.Veo config = properties.getProviders().getVeo();
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("prompt", buildVeoPrompt(job, profile, script, scene, sceneCount));
        if (sourceImage != null) {
            instance.put("image", Map.of(
                    "mimeType", sourceImage.mimeType(),
                    "bytesBase64Encoded", Base64.getEncoder().encodeToString(sourceImage.content())));
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("aspectRatio", config.getAspectRatio());
        parameters.put("resolution", config.getResolution());
        parameters.put("personGeneration", sourceImage == null ? config.getPersonGeneration() : "allow_adult");
        parameters.put("durationSeconds", config.getDurationSeconds());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instances", java.util.List.of(instance));
        payload.put("parameters", parameters);

        JsonNode response;
        try {
            response = authorized(webClient.post()
                            .uri("/models/{model}:predictLongRunning", config.getModel())
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException ex) {
            String body = sanitizeProviderBody(ex.getResponseBodyAsString());
            logProviderError(job, scene, ex, body);
            String code = ex.getStatusCode().value() == 429 ? "PROVIDER_RATE_LIMIT" : "PROVIDER_RENDER_FAILED";
            throw new VideoProviderException(
                    code,
                    "VEO retornou HTTP %d ao criar cena %d: %s"
                            .formatted(ex.getStatusCode().value(), scene, body),
                    ex);
        }
        String operationName = response != null ? response.path("name").asText(null) : null;
        if (!StringUtils.hasText(operationName)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "VEO não retornou o nome da operação para o job " + job.id());
        }
        return operationName;
    }

    /** Registra o erro completo do provider sem incluir imagem base64 ou credencial. */
    private void logProviderError(SalesVideoJob job,
                                  int scene,
                                  WebClientResponseException ex,
                                  String responseBody) {
        VideoManagementProperties.Veo config = properties.getProviders().getVeo();
        log.warn(
                "VEO recusou criação; jobId={} scene={} model={} status={} url={} responseBody={}",
                job.id(),
                scene,
                config.getModel(),
                ex.getStatusCode().value(),
                resolveBaseUrl() + "/models/" + config.getModel() + ":predictLongRunning",
                responseBody,
                ex);
    }

    /** Limita o corpo externo antes de persistir ou registrar o diagnóstico. */
    private String sanitizeProviderBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "sem corpo";
        }
        String normalized = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 1200 ? normalized.substring(0, 1200) : normalized;
    }

    /** Aguarda a operação assíncrona do VEO concluir dentro do limite configurado. */
    private JsonNode waitUntilDone(String operationName,
                                   int scene,
                                   int sceneCount,
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
                    "VEO processando cena %d/%d (tentativa %d/%d)"
                            .formatted(scene, sceneCount, attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão do VEO");
    }

    /** Baixa o arquivo final informado pela Gemini API usando a mesma chave de autenticação. */
    private ProviderFile downloadVideo(SalesVideoJob job, String videoUri, int scene) {
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
        return new ProviderFile("sales-video-" + job.id() + "-veo-scene-" + scene + ".mp4",
                VIDEO_MP4,
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                content);
    }

    /** Monta o prompt versionado e específico da cena a partir do contrato comercial persistido. */
    private String buildVeoPrompt(SalesVideoJob job,
                                  SalesVideoProfile profile,
                                  SalesVideoScript script,
                                  int scene,
                                  int sceneCount) {
        JsonNode metadata = readMetadata(job);
        try {
            return new ClassPathResource(COMMERCIAL_PROMPT_PATH)
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{SCENE_NUMBER}}", String.valueOf(scene))
                    .replace("{{SCENE_COUNT}}", String.valueOf(sceneCount))
                    .replace("{{SCENE_BRIEF}}", compactPromptField(plannedSceneBrief(metadata, scene, sceneCount), 1000))
                    .replace("{{LANGUAGE}}", compactPromptField(nullToDefault(profile.language(), "pt-BR"), 16))
                    .replace("{{TITLE}}", compactPromptField(nullToDefault(profile.title(), "Vídeo comercial"), 100))
                    .replace("{{PERSONA}}", compactPromptField(nullToDefault(profile.personaName(), "público-alvo"), 100))
                    .replace("{{VOICE_STYLE}}", compactPromptField(nullToDefault(profile.voiceStyle(), "confiante"), 100))
                    .replace("{{CHARACTER_PROMPT}}", compactPromptField(
                            metadata.path("characterImagePrompt").asText("preservar a pessoa da imagem aprovada"), 180))
                    .replace("{{CHARACTER_REFERENCE_URL}}", compactPromptField(
                            metadata.path("characterImageReferenceUrl").asText("imagem incorporada ao request"), 220))
                    .replace("{{HOOK}}", compactPromptField(nullToDefault(script.hookText(), ""), 160))
                    .replace("{{SCRIPT}}", compactPromptField(script.scriptText(), 320))
                    .replace("{{CTA}}", compactPromptField(nullToDefault(script.ctaText(), ""), 120))
                    .replace("{{VISUAL_DIRECTIVES}}", compactPromptField(visualProviderDirectives(metadata), 350));
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível carregar prompt comercial VEO", ex);
        }
    }

    /** Seleciona os cortes do clipe atual sem enviar o histórico completo ao provider. */
    private String plannedSceneBrief(JsonNode metadata, int scene, int sceneCount) {
        JsonNode isolated = metadata.path("scene");
        if ("SCENE_BY_SCENE_MONTAGE".equalsIgnoreCase(metadata.path("generation_strategy").asText(""))
                && isolated.isObject()) {
            return compactPromptField(isolated.toString(), 1000);
        }
        JsonNode cuts = metadata.path("cut_plan");
        if (!cuts.isArray() || cuts.isEmpty()) {
            return "Recognizable pain, practical mechanism, plausible transformation and clear next step.";
        }
        int start = (scene - 1) * cuts.size() / sceneCount;
        int end = Math.max(start + 1, scene * cuts.size() / sceneCount);
        List<String> selected = new ArrayList<>();
        for (int index = start; index < Math.min(end, cuts.size()); index++) {
            JsonNode cut = cuts.get(index);
            selected.add("Cut %d (%s): %s Continuity: %s".formatted(
                    cut.path("order").asInt(index + 1),
                    cut.path("role").asText("MECANISMO"),
                    compactPromptField(cut.path("visual_objective").asText("single visual action"), 220),
                    compactPromptField(cut.path("continuity_anchor").asText("same person and wardrobe"), 80)));
        }
        return String.join(" ", selected);
    }

    /** Normaliza e limita campos extensos sem cortar as instruções fixas do prompt. */
    private String compactPromptField(String value, int maxCharacters) {
        String normalized = nullToDefault(value, "").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxCharacters - 1)).trim() + "…";
    }

    /** Resolve e limita a quantidade de cenas pagas declarada pelo Estúdio. */
    private int resolveSceneCount(JsonNode metadata) {
        int requested = Math.max(1, metadata.path("sceneCount").asInt(1));
        if (requested > MAX_SCENES_PER_JOB) {
            throw new VideoProviderException(
                    "PROVIDER_INVALID_REQUEST",
                    "VEO aceita no máximo %d cenas por job governado".formatted(MAX_SCENES_PER_JOB));
        }
        return requested;
    }

    /** Impede chamadas pagas acima do teto aprovado e persistido no ciclo. */
    private void ensureBudget(JsonNode metadata, BigDecimal estimatedCost) {
        JsonNode budgetNode = metadata.path("budgetLimitUsd");
        if (budgetNode.isMissingNode() || budgetNode.isNull()) {
            return;
        }
        try {
            BigDecimal budget = new BigDecimal(budgetNode.asText());
            if (budget.signum() >= 0 && estimatedCost.compareTo(budget) > 0) {
                throw new VideoProviderException(
                        "PROVIDER_BUDGET_EXCEEDED",
                        "Custo VEO estimado em US$ %s excede teto de US$ %s"
                                .formatted(estimatedCost, budget));
            }
        } catch (NumberFormatException ex) {
            throw new VideoProviderException(
                    "PROVIDER_INVALID_REQUEST", "budgetLimitUsd inválido no job VEO", ex);
        }
    }

    /** Baixa uma única vez a imagem aprovada que inicia todas as cenas do job. */
    private InputImage loadSourceImage(JsonNode metadata) {
        String imageUrl = firstText(metadata,
                "/image_to_video/source_image_url",
                "/image_to_video/reference_image_url");
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        MediaType contentType = response == null ? null : response.getHeaders().getContentType();
        if (content == null || content.length == 0 || contentType == null
                || !"image".equalsIgnoreCase(contentType.getType())) {
            throw new VideoProviderException(
                    "PROVIDER_INVALID_REQUEST", "Imagem-base do VEO não retornou conteúdo de imagem válido");
        }
        return new InputImage(contentType.toString(), content);
    }

    /** Rejeita operação concluída com erro antes de procurar o vídeo gerado. */
    private void ensureSuccessfulStatus(JsonNode finalStatus) {
        JsonNode error = finalStatus.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new VideoProviderException(
                    "PROVIDER_RENDER_FAILED",
                    "VEO retornou erro: " + error.path("message").asText(error.toString()));
        }
    }

    /** Extrai a URI final do vídeo ou falha com causa explícita. */
    private String resolveVideoUri(JsonNode finalStatus) {
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
        return videoUri;
    }

    /** Preserva status bruto, número da cena e operação externa para auditoria. */
    private Map<String, Object> sceneMetadata(int scene, String operationName, JsonNode finalStatus) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scene_number", scene);
        metadata.put("provider_operation", operationName);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        return metadata;
    }

    /** Concatena cenas VEO homogêneas sem recodificação e preserva o MP4 integral. */
    private ProviderFile assembleScenes(SalesVideoJob job, List<ProviderFile> clips) {
        List<Path> temporaryFiles = new ArrayList<>();
        try {
            Path manifest = Files.createTempFile("veo-scenes-" + job.id(), ".txt");
            temporaryFiles.add(manifest);
            StringBuilder entries = new StringBuilder();
            for (ProviderFile clip : clips) {
                Path file = Files.createTempFile("veo-scene-" + job.id(), ".mp4");
                Files.write(file, clip.content());
                temporaryFiles.add(file);
                entries.append("file '").append(file.toAbsolutePath()).append("'\n");
            }
            Files.writeString(manifest, entries);
            Path output = Files.createTempFile("veo-montage-" + job.id(), ".mp4");
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
                throw new VideoProviderException(
                        "VIDEO_ASSEMBLY_FAILED",
                        "ffmpeg falhou ao montar cenas VEO; exitCode=%d output=%s"
                                .formatted(exitCode, new String(processOutput, StandardCharsets.UTF_8)));
            }
            return new ProviderFile(
                    "sales-video-" + job.id() + "-veo.mp4",
                    VIDEO_MP4,
                    AssetType.VIDEO,
                    ProviderAssetRole.VIDEO,
                    Files.readAllBytes(output));
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Falha ao montar cenas VEO", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Montagem VEO interrompida", ex);
        } finally {
            temporaryFiles.forEach(this::deleteIfExists);
        }
    }

    /** Remove somente arquivos temporários criados pelo job VEO atual. */
    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A limpeza é best effort e nunca altera o resultado comercial já produzido.
        }
    }

    /** Consolida custo, modelo, cenas e origem da imagem sem persistir bytes no metadata. */
    private Map<String, Object> metadata(SalesVideoJob job,
                                         String providerJobId,
                                         int sceneCount,
                                         BigDecimal estimatedCost,
                                         List<Map<String, Object>> scenes,
                                         InputImage sourceImage) {
        VideoManagementProperties.Veo config = properties.getProviders().getVeo();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "VEO");
        metadata.put("provider_job_id", providerJobId);
        metadata.put("model", config.getModel());
        metadata.put("aspect_ratio", config.getAspectRatio());
        metadata.put("resolution", config.getResolution());
        metadata.put("clip_duration_seconds", config.getDurationSeconds());
        metadata.put("duration_seconds", config.getDurationSeconds() * sceneCount);
        metadata.put("scene_count", sceneCount);
        metadata.put("assembled_locally", sceneCount > 1);
        metadata.put("modality", sourceImage == null ? "text_to_video" : "image_to_video");
        metadata.put("cost_usd", estimatedCost);
        metadata.put("cost_scope", "ALL_SCENES");
        metadata.put("pricing_source", "Google Gemini API pricing: Veo video generation billed per generated second");
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("scenes", scenes);
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Serializa a reserva ou liquidação financeira de uma operação VEO. */
    private String providerTaskDetails(String eventType,
                                       String operationName,
                                       int scene,
                                       int sceneCount,
                                       BigDecimal costUsd) {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("eventType", eventType);
            details.put("provider", "VEO");
            details.put("providerTaskId", operationName);
            details.put("model", properties.getProviders().getVeo().getModel());
            details.put("sceneNumber", scene);
            details.put("plannedSceneCount", sceneCount);
            if ("PROVIDER_TASK_ACCEPTED".equals(eventType)) {
                details.put("estimatedCostUsd", costUsd);
                details.put("estimatedCredits", costUsd.movePointRight(2).intValue());
            } else {
                details.put("billedCostUsd", costUsd);
                details.put("billedCredits", costUsd.movePointRight(2).intValue());
                details.put("settlementStatus", "SUCCEEDED");
                details.put("settlementBasis", "PROVIDER_COMPLETION_WITH_CATALOG_PRICE");
                details.put("billingEvidence", "VEO concluiu a operação; custo conciliado pela tabela do adapter.");
            }
            return objectMapper.writeValueAsString(details);
        } catch (IOException ex) {
            throw new VideoProviderException("PROVIDER_AUDIT_FAILED", "Falha ao auditar consumo VEO", ex);
        }
    }

    /** Resolve o primeiro texto útil entre os JSON pointers fornecidos. */
    private String firstText(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode value = node.at(pointer);
            if (!value.isMissingNode() && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    /** Mantém MIME e bytes da imagem aprovada apenas durante a chamada ao provider. */
    private record InputImage(String mimeType, byte[] content) { }

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
