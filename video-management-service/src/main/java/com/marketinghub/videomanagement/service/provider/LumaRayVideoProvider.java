package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Adapter direto para gerar e montar vídeos comerciais com Luma Ray 3.2. */
@Component
@ConditionalOnProperty(prefix = "video.providers.luma", name = "enabled", havingValue = "true")
public class LumaRayVideoProvider implements VideoProvider {
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final MediaType IMAGE_PNG = MediaType.IMAGE_PNG;
    private static final int MAX_VIDEO_DOWNLOAD_BYTES = 100 * 1024 * 1024;
    private static final String REFERENCE_IMAGE_PROMPT_PATH = "prompts/sales-video/openai-reference-image.md";
    private static final String SERVICE_TIER = "flex";

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final VideoAssetClient assetClient;
    private final WebClient webClient;
    private final WebClient openAiWebClient;
    private final WebClient downloadWebClient;

    /** Inicializa o provider Luma com configuração, mapper JSON e WebClient. */
    public LumaRayVideoProvider(VideoManagementProperties properties,
                                ObjectMapper objectMapper,
                                VideoAssetClient assetClient,
                                WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.assetClient = assetClient;
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
        this.openAiWebClient = webClientBuilder.clone()
                .baseUrl(properties.getProviders().getLuma().getOpenAiBaseUrl().toString())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_VIDEO_DOWNLOAD_BYTES))
                        .build())
                .build();
    }

    /** Verifica se o job de render pertence ao provider Luma Ray 3.2. */
    @Override
    public boolean supports(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.RENDER) {
            return false;
        }
        String providerName = normalize(job.providerName());
        if (!StringUtils.hasText(providerName)) {
            return false;
        }
        return properties.getProviders().getLuma().getAcceptedNames().stream()
                .map(this::normalize)
                .anyMatch(providerName::equals);
    }

    /** Gera cenas Luma de 10s, monta um MP4 final e devolve metadados auditáveis ao backend. */
    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        requireApiKey();
        SalesVideoScript script = ensureScript(profile);
        int sceneCount = Math.max(1, properties.getProviders().getLuma().getSceneCount());
        List<Path> sceneFiles = new ArrayList<>();
        List<Map<String, Object>> sceneMetadata = new ArrayList<>();
        try {
            for (int sceneIndex = 1; sceneIndex <= sceneCount; sceneIndex++) {
                int startProgress = 5 + ((sceneIndex - 1) * 25);
                progressCallback.onProgress(startProgress, SalesVideoStatus.VIDEO_PROCESSING,
                        "Enviando cena %d/%d para Luma Ray 3.2".formatted(sceneIndex, sceneCount));
                List<Map<String, Object>> referenceImages = generateReferenceImagesIfRequested(
                        job,
                        profile,
                        script,
                        sceneIndex,
                        sceneCount);
                String generationId = submitScene(job, profile, script, sceneIndex, sceneCount, referenceImages);
                JsonNode finalStatus = waitUntilCompleted(generationId, sceneIndex, sceneCount, progressCallback);
                String videoUrl = resolveVideoUrl(finalStatus);
                if (!StringUtils.hasText(videoUrl)) {
                    throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                            "Luma não retornou URL de vídeo para a cena " + sceneIndex);
                }
                Path sceneFile = downloadScene(job, sceneIndex, videoUrl);
                sceneFiles.add(sceneFile);
                sceneMetadata.add(sceneMetadata(sceneIndex, generationId, finalStatus, referenceImages));
            }
            progressCallback.onProgress(86, SalesVideoStatus.VIDEO_PROCESSING, "Montando vídeo final Luma");
            ProviderFile video = assembleFinalVideo(job, sceneFiles);
            Map<String, Object> metadata = metadata(job, sceneCount, sceneMetadata);
            progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Luma finalizado com MP4 montado");
            return new ProviderArtifacts(joinProviderJobIds(sceneMetadata), video, null, null, metadata);
        } finally {
            cleanup(sceneFiles);
        }
    }

    /** Cria uma geração de cena na Luma Agents API. */
    private String submitScene(SalesVideoJob job,
                               SalesVideoProfile profile,
                               SalesVideoScript script,
                               int sceneIndex,
                               int sceneCount,
                               List<Map<String, Object>> referenceImages) {
        VideoManagementProperties.Luma config = properties.getProviders().getLuma();
        Map<String, Object> video = new LinkedHashMap<>();
        video.put("resolution", config.getResolution());
        video.put("duration", config.getDuration());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.getModel());
        payload.put("type", "video");
        payload.put("prompt", buildScenePrompt(job, profile, script, sceneIndex, sceneCount));
        payload.put("aspect_ratio", config.getAspectRatio());
        payload.put("video", video);
        if (!referenceImages.isEmpty()) {
            payload.put("keyframes", lumaKeyframes(referenceImages));
        }

        JsonNode response = authorized(webClient.post()
                        .uri("/v1/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        String generationId = response == null ? null : response.path("id").asText(null);
        if (!StringUtils.hasText(generationId)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Luma não retornou id de geração para o job " + job.id());
        }
        return generationId;
    }

    /** Gera e publica imagens OpenAI para usar como keyframes Luma quando o job solicitar esse modo. */
    private List<Map<String, Object>> generateReferenceImagesIfRequested(SalesVideoJob job,
                                                                          SalesVideoProfile profile,
                                                                          SalesVideoScript script,
                                                                          int sceneIndex,
                                                                          int sceneCount) {
        JsonNode metadata = readMetadata(job);
        JsonNode imageToVideo = metadata.path("image_to_video");
        boolean enabled = imageToVideo.path("enabled").asBoolean(false)
                || "OPENAI_IMAGE_TO_LUMA_VIDEO".equalsIgnoreCase(metadata.path("generation_strategy").asText(""));
        if (!enabled) {
            return List.of();
        }
        requireOpenAiImageApiKey();
        int count = Math.max(1, Math.min(2, imageToVideo.path("reference_image_count").asInt(1)));
        List<Map<String, Object>> references = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            String prompt = buildReferenceImagePrompt(job, profile, script, sceneIndex, sceneCount, index, count);
            byte[] image = generateOpenAiReferenceImage(prompt);
            ProviderFile file = new ProviderFile(
                    "sales-video-" + job.id() + "-luma-reference-scene-" + sceneIndex + "-" + index + ".png",
                    IMAGE_PNG,
                    AssetType.IMAGE,
                    ProviderAssetRole.IMAGE,
                    image);
            String metadataJson = writeJson(Map.of(
                    "job_id", job.id(),
                    "profile_id", job.profileId(),
                    "script_id", job.scriptId(),
                    "role", "LUMA_REFERENCE_IMAGE",
                    "scene_index", sceneIndex,
                    "reference_index", index,
                    "provider", "OPENAI",
                    "model", properties.getProviders().getLuma().getOpenAiImageModel(),
                    "image_tool_model", properties.getProviders().getLuma().getOpenAiImageToolModel(),
                    "service_tier", SERVICE_TIER));
            var asset = assetClient.uploadAsset(file, metadataJson);
            if (asset == null || !StringUtils.hasText(asset.url())) {
                throw new VideoProviderException("VIDEO_REFERENCE_IMAGE_FAILED",
                        "Imagem OpenAI foi gerada, mas o backend não retornou URL pública");
            }
            references.add(Map.of(
                    "reference_index", index,
                    "asset_id", asset.id(),
                    "url", asset.url(),
                    "model", properties.getProviders().getLuma().getOpenAiImageModel(),
                    "image_tool_model", properties.getProviders().getLuma().getOpenAiImageToolModel(),
                    "service_tier", SERVICE_TIER));
        }
        return references;
    }

    /** Monta keyframes no contrato aceito pela Luma para imagem inicial e, opcionalmente, final. */
    private Map<String, Object> lumaKeyframes(List<Map<String, Object>> referenceImages) {
        Map<String, Object> keyframes = new LinkedHashMap<>();
        if (!referenceImages.isEmpty()) {
            keyframes.put("frame0", Map.of("type", "image", "url", referenceImages.getFirst().get("url")));
        }
        if (referenceImages.size() > 1) {
            keyframes.put("frame1", Map.of("type", "image", "url", referenceImages.get(1).get("url")));
        }
        return keyframes;
    }

    /** Gera imagem base64 via Responses API com ferramenta de geração de imagem e modo Flex. */
    private byte[] generateOpenAiReferenceImage(String prompt) {
        VideoManagementProperties.Luma config = properties.getProviders().getLuma();
        Map<String, Object> payload = Map.of(
                "model", config.getOpenAiImageModel(),
                "input", prompt,
                "service_tier", SERVICE_TIER,
                "tool_choice", Map.of("type", "image_generation"),
                "tools", List.of(Map.of(
                        "type", "image_generation",
                        "action", "generate",
                        "model", config.getOpenAiImageToolModel(),
                        "output_format", "png")));
        JsonNode response = openAiWebClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveOpenAiApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return Base64.getDecoder().decode(extractOpenAiImageBase64(response));
    }

    /** Extrai o PNG base64 retornado pela ferramenta image_generation. */
    private String extractOpenAiImageBase64(JsonNode response) {
        JsonNode output = response == null ? null : response.path("output");
        if (output == null || !output.isArray()) {
            throw new VideoProviderException("VIDEO_REFERENCE_IMAGE_FAILED", "OpenAI não retornou output de imagem");
        }
        for (JsonNode item : output) {
            if ("image_generation_call".equals(item.path("type").asText())
                    && StringUtils.hasText(item.path("result").asText())) {
                return item.path("result").asText();
            }
        }
        throw new VideoProviderException("VIDEO_REFERENCE_IMAGE_FAILED", "OpenAI não retornou imagem base64");
    }

    /** Monta prompt versionado da imagem de referência com a cena e a estratégia comercial. */
    private String buildReferenceImagePrompt(SalesVideoJob job,
                                             SalesVideoProfile profile,
                                             SalesVideoScript script,
                                             int sceneIndex,
                                             int sceneCount,
                                             int referenceIndex,
                                             int referenceCount) {
        JsonNode metadata = readMetadata(job);
        String sceneBrief = sceneBrief(metadata.path("assembly_plan").path("scenes"), sceneIndex, sceneCount);
        String imagePrompt = metadata.path("image_to_video").path("image_prompt").asText("");
        String commercialGoal = StringUtils.hasText(imagePrompt)
                ? imagePrompt.trim()
                : "Criar quadro-base funcional, anti-sensualizacao, para conduzir a cliente da dor reconhecivel ao desejo de ver o Plano MUSA.";
        if (referenceCount > 1) {
            commercialGoal = commercialGoal + " Reference frame " + referenceIndex
                    + " of " + referenceCount + ": "
                    + (referenceIndex == 1 ? "inicio da microacao, ainda com duvida."
                    : "fim da microacao, com alivio e clareza.");
        }
        try {
            ClassPathResource resource = new ClassPathResource(REFERENCE_IMAGE_PROMPT_PATH);
            return resource.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{COMMERCIAL_GOAL}}", commercialGoal)
                    .replace("{{SCENE_BRIEF}}", sceneBrief)
                    .replace("{{APPROVED_SCRIPT}}", script.scriptText())
                    .replace("{{VISUAL_DIRECTIVES}}", visualProviderDirectives(metadata));
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível carregar prompt de imagem-base OpenAI", ex);
        }
    }

    /** Aguarda a cena chegar em completed ou falha objetivamente. */
    private JsonNode waitUntilCompleted(String generationId,
                                        int sceneIndex,
                                        int sceneCount,
                                        ProgressCallback progressCallback) {
        VideoManagementProperties.Luma config = properties.getProviders().getLuma();
        for (int attempt = 1; attempt <= config.getMaxPollAttempts(); attempt++) {
            JsonNode status = authorized(webClient.get().uri("/v1/generations/{generationId}", generationId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            String state = normalize(status == null ? null : status.path("state").asText(null));
            if ("COMPLETED".equals(state)) {
                return status;
            }
            if ("FAILED".equals(state)) {
                throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                        "Luma falhou na cena %d: %s".formatted(sceneIndex, readFailure(status)));
            }
            int progress = Math.min(84, 10 + ((sceneIndex - 1) * 25) + attempt);
            progressCallback.onProgress(progress, SalesVideoStatus.VIDEO_PROCESSING,
                    "Luma processando cena %d/%d (tentativa %d/%d)"
                            .formatted(sceneIndex, sceneCount, attempt, config.getMaxPollAttempts()));
            sleep(config.getPollInterval().toMillis());
        }
        throw new VideoProviderException("PROVIDER_TIMEOUT", "Timeout aguardando conclusão da Luma");
    }

    /** Baixa uma cena MP4 gerada pela Luma. */
    private Path downloadScene(SalesVideoJob job, int sceneIndex, String videoUrl) {
        ResponseEntity<byte[]> response = downloadWebClient.get()
                .uri(URI.create(videoUrl))
                .retrieve()
                .toEntity(byte[].class)
                .block();
        byte[] content = response == null ? null : response.getBody();
        if (content == null || content.length == 0) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Download vazio da cena Luma " + sceneIndex);
        }
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null || !VIDEO_MP4.isCompatibleWith(contentType) || !looksLikeMp4(content)) {
            throw new VideoProviderException("PROVIDER_RENDER_FAILED",
                    "Download da Luma não retornou MP4 válido; contentType=%s bytes=%d"
                            .formatted(contentType, content.length));
        }
        try {
            Path scene = Files.createTempFile("sales-video-" + job.id() + "-luma-scene-" + sceneIndex, ".mp4");
            Files.write(scene, content);
            return scene;
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_MODULE_ERROR", "Falha ao salvar cena Luma", ex);
        }
    }

    /** Monta o MP4 final a partir das cenas geradas. */
    private ProviderFile assembleFinalVideo(SalesVideoJob job, List<Path> sceneFiles) {
        if (sceneFiles.size() == 1) {
            try {
                return new ProviderFile("sales-video-" + job.id() + "-luma.mp4",
                        VIDEO_MP4,
                        AssetType.VIDEO,
                        ProviderAssetRole.VIDEO,
                        Files.readAllBytes(sceneFiles.getFirst()));
            } catch (IOException ex) {
                throw new VideoProviderException("VIDEO_MODULE_ERROR", "Falha ao ler cena única Luma", ex);
            }
        }
        Path concatList = null;
        Path output = null;
        try {
            concatList = Files.createTempFile("sales-video-" + job.id() + "-luma-concat", ".txt");
            output = Files.createTempFile("sales-video-" + job.id() + "-luma-final", ".mp4");
            StringBuilder list = new StringBuilder();
            for (Path scene : sceneFiles) {
                list.append("file '").append(scene.toAbsolutePath()).append("'\n");
            }
            Files.writeString(concatList, list.toString());
            Process process = new ProcessBuilder(
                    properties.getProviders().getLuma().getFfmpegPath(),
                    "-y",
                    "-f", "concat",
                    "-safe", "0",
                    "-i", concatList.toAbsolutePath().toString(),
                    "-c", "copy",
                    output.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED",
                        "ffmpeg falhou ao montar vídeo Luma; exitCode=" + exitCode);
            }
            byte[] content = Files.readAllBytes(output);
            return new ProviderFile("sales-video-" + job.id() + "-luma.mp4",
                    VIDEO_MP4,
                    AssetType.VIDEO,
                    ProviderAssetRole.VIDEO,
                    content);
        } catch (IOException ex) {
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Falha ao executar ffmpeg para Luma", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Montagem Luma interrompida", ex);
        } finally {
            deleteIfExists(concatList);
            deleteIfExists(output);
        }
    }

    /** Monta o prompt de uma cena a partir do roteiro e dos metadados comerciais. */
    private String buildScenePrompt(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    SalesVideoScript script,
                                    int sceneIndex,
                                    int sceneCount) {
        JsonNode metadata = readMetadata(job);
        JsonNode scenes = metadata.path("assembly_plan").path("scenes");
        String sceneBrief = sceneBrief(scenes, sceneIndex, sceneCount);
        String visualDirectives = visualProviderDirectives(metadata);
        return """
                Vertical 9:16 commercial video scene for Método MUSA - Presença Elegante em 7 Dias.
                Scene %d of %d. Scene brief: %s.
                Product promise: presence that feels more elegant, intentional and coherent in 7 days, using accessible micro-actions and what the woman already owns.
                Audience: Brazilian urban women who want sophistication without luxury pressure or excessive effort.
                Visual style: realistic editorial, warm natural light, vinho #7A2444, creme #FFF8F3, blush #F3C9C1, subtle gold #D6A75C, urban feminine, intimate, premium but accessible.
                Provider-specific visual directives: %s.
                Avoid embedded text, logos, distorted phone UI, luxury ostentation, shame, guaranteed transformation or universal approval claims.
                Approved hook: %s.
                Approved CTA: %s.
                Approved script context: %s.
                Keep the scene self-contained, cinematic, human, emotionally clear and suitable for later subtitles in Portuguese.
                """.formatted(
                sceneIndex,
                sceneCount,
                sceneBrief,
                visualDirectives,
                nullToDefault(script.hookText(), ""),
                nullToDefault(script.ctaText(), ""),
                script.scriptText());
    }

    /** Extrai diretivas visuais do metadata para orientar nitidez, luz e composição no provider. */
    private String visualProviderDirectives(JsonNode metadata) {
        String directives = metadata.path("visual_provider_directives").asText("");
        if (StringUtils.hasText(directives)) {
            return directives.trim();
        }
        return "Sharp image, crisp focus, stable exposure, constant natural light, no haze, no blur and no flickering.";
    }

    /** Resume as cenas do plano de montagem em blocos compatíveis com 3 gerações de 10s. */
    private String sceneBrief(JsonNode scenes, int sceneIndex, int sceneCount) {
        if (!scenes.isArray() || scenes.isEmpty()) {
            return "Dor, result, mechanism and CTA in a concise editorial MUSA sequence.";
        }
        int total = scenes.size();
        int start = Math.max(0, (sceneIndex - 1) * total / sceneCount);
        int end = Math.max(start + 1, sceneIndex * total / sceneCount);
        List<String> briefs = new ArrayList<>();
        for (int i = start; i < Math.min(end, total); i++) {
            JsonNode scene = scenes.get(i);
            briefs.add("%s - %s: %s".formatted(
                    scene.path("role").asText("SCENE"),
                    scene.path("title").asText(""),
                    scene.path("message").asText("")));
        }
        return String.join(" | ", briefs);
    }

    /** Extrai a URL final de vídeo nos formatos atuais da Luma Agents API. */
    private String resolveVideoUrl(JsonNode finalStatus) {
        JsonNode output = finalStatus.path("output");
        if (output.isArray() && !output.isEmpty()) {
            String direct = output.path(0).path("url").asText(null);
            if (StringUtils.hasText(direct)) {
                return direct;
            }
        }
        return finalStatus.path("assets").path("video").asText(null);
    }

    /** Gera metadados de uma cena para auditoria. */
    private Map<String, Object> sceneMetadata(int sceneIndex,
                                              String generationId,
                                              JsonNode finalStatus,
                                              List<Map<String, Object>> referenceImages) {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_index", sceneIndex);
        scene.put("generation_id", generationId);
        scene.put("state", finalStatus.path("state").asText(null));
        scene.put("model", finalStatus.path("model").asText(properties.getProviders().getLuma().getModel()));
        if (!referenceImages.isEmpty()) {
            scene.put("reference_images", referenceImages);
        }
        return scene;
    }

    /** Consolida metadados finais do render Luma. */
    private Map<String, Object> metadata(SalesVideoJob job, int sceneCount, List<Map<String, Object>> scenes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "LUMA_RAY_3_2");
        metadata.put("provider_job_id", joinProviderJobIds(scenes));
        metadata.put("model", properties.getProviders().getLuma().getModel());
        metadata.put("aspect_ratio", properties.getProviders().getLuma().getAspectRatio());
        metadata.put("resolution", properties.getProviders().getLuma().getResolution());
        metadata.put("duration_seconds", sceneCount * parseDurationSeconds(properties.getProviders().getLuma().getDuration()));
        metadata.put("scene_count", sceneCount);
        metadata.put("scenes", scenes);
        metadata.put("image_to_video_enabled", scenes.stream().anyMatch(scene -> scene.containsKey("reference_images")));
        metadata.put("cost_usd", estimateCostUsd(sceneCount));
        metadata.put("pricing_source", "Luma Agents pricing: ray-3.2 standard video generation by resolution and duration");
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Junta ids de geração para preservar rastreabilidade no campo providerJobId. */
    private String joinProviderJobIds(List<Map<String, Object>> scenes) {
        return scenes.stream()
                .map(scene -> String.valueOf(scene.get("generation_id")))
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    /** Calcula custo aproximado por tabela pública da Luma Agents. */
    private BigDecimal estimateCostUsd(int sceneCount) {
        String resolution = normalize(properties.getProviders().getLuma().getResolution());
        int duration = parseDurationSeconds(properties.getProviders().getLuma().getDuration());
        BigDecimal sceneCost;
        if ("1080P".equals(resolution)) {
            sceneCost = duration >= 10 ? new BigDecimal("3.6000") : new BigDecimal("1.2000");
        } else if ("720P".equals(resolution)) {
            sceneCost = duration >= 10 ? new BigDecimal("0.9000") : new BigDecimal("0.3000");
        } else if ("540P".equals(resolution)) {
            sceneCost = duration >= 10 ? new BigDecimal("0.4500") : new BigDecimal("0.1500");
        } else {
            sceneCost = duration >= 10 ? new BigDecimal("0.1800") : new BigDecimal("0.0600");
        }
        return sceneCost.multiply(BigDecimal.valueOf(sceneCount));
    }

    /** Converte duração textual da Luma, como 10s, para segundos. */
    private int parseDurationSeconds(String duration) {
        String normalized = normalize(duration).replace("S", "");
        return normalized.matches("\\d+") ? Integer.parseInt(normalized) : 10;
    }

    /** Lê metadados JSON do job sem quebrar jobs antigos vazios. */
    private JsonNode readMetadata(SalesVideoJob job) {
        if (!StringUtils.hasText(job.metadataJson())) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(job.metadataJson());
        } catch (IOException ex) {
            throw new VideoProviderException("PROVIDER_INVALID_REQUEST",
                    "metadataJson inválido para job Luma " + job.id(), ex);
        }
    }

    /** Valida existência de script aprovado no perfil. */
    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização Luma");
        }
        return script;
    }

    /** Exige chave Luma antes de chamar API externa. */
    private void requireApiKey() {
        if (!StringUtils.hasText(resolveApiKey())) {
            throw new VideoProviderException("PROVIDER_AUTH_ERROR", "LUMA_AGENTS_API_KEY não configurada para Luma");
        }
    }

    /** Exige chave OpenAI quando o modo imagem-para-vídeo for solicitado. */
    private void requireOpenAiImageApiKey() {
        VideoManagementProperties.Luma config = properties.getProviders().getLuma();
        if (!config.isOpenAiReferenceImageEnabled()) {
            throw new VideoProviderException("VIDEO_REFERENCE_IMAGE_FAILED",
                    "Geração de imagem OpenAI para Luma não está habilitada no video-management-service");
        }
        if (!StringUtils.hasText(resolveOpenAiApiKey())) {
            throw new VideoProviderException("PROVIDER_AUTH_ERROR",
                    "OPENAI_API_KEY não configurada para imagem-base do Luma");
        }
    }

    /** Aplica autorização Bearer em chamadas Luma. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveApiKey());
        request.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    /** Resolve chave Luma por variável direta ou arquivo de secret. */
    private String resolveApiKey() {
        VideoManagementProperties.Luma config = properties.getProviders().getLuma();
        if (StringUtils.hasText(config.getApiKey())) {
            return config.getApiKey().trim();
        }
        if (!StringUtils.hasText(config.getApiKeyFile())) {
            return "";
        }
        try {
            Path path = Path.of(config.getApiKeyFile());
            if (!Files.isReadable(path)) {
                return "";
            }
            return Files.readString(path).trim();
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível ler LUMA_API_KEY_FILE", ex);
        }
    }

    /** Resolve chave OpenAI por variável direta ou arquivo de secret. */
    private String resolveOpenAiApiKey() {
        VideoManagementProperties.Luma config = properties.getProviders().getLuma();
        if (StringUtils.hasText(config.getOpenAiApiKey())) {
            return config.getOpenAiApiKey().trim();
        }
        if (!StringUtils.hasText(config.getOpenAiApiKeyFile())) {
            return "";
        }
        try {
            Path path = Path.of(config.getOpenAiApiKeyFile().trim());
            if (!Files.isReadable(path)) {
                return "";
            }
            return Files.readString(path).trim();
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível ler OPENAI_API_KEY_FILE", ex);
        }
    }

    /** Serializa metadados de imagem sem expor payload sensível. */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível serializar metadata de imagem-base", ex);
        }
    }

    /** Resolve base URL da API Luma. */
    private String resolveBaseUrl() {
        return properties.getProviders().getLuma().getBaseUrl().toString();
    }

    /** Lê motivo de falha retornado pela Luma. */
    private String readFailure(JsonNode status) {
        if (status == null) {
            return "resposta vazia";
        }
        String reason = status.path("failure_reason").asText(null);
        return StringUtils.hasText(reason) ? reason : status.toString();
    }

    /** Verifica assinatura mínima MP4. */
    private boolean looksLikeMp4(byte[] content) {
        if (content.length < 12) {
            return false;
        }
        return content[4] == 'f' && content[5] == 't' && content[6] == 'y' && content[7] == 'p';
    }

    /** Retorna fallback quando o valor está vazio. */
    private String nullToDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    /** Normaliza textos de provider e configuração. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /** Pausa entre tentativas de polling. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProviderException("Thread interrompida durante polling da Luma", ex);
        }
    }

    /** Remove arquivos temporários de cenas. */
    private void cleanup(List<Path> sceneFiles) {
        sceneFiles.forEach(this::deleteIfExists);
    }

    /** Remove arquivo temporário sem falhar o job por limpeza. */
    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // Limpeza temporária não deve mascarar o resultado do render.
        }
    }
}
