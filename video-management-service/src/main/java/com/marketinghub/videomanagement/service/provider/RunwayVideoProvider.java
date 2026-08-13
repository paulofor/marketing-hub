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
import java.util.ArrayList;
import java.util.List;
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

        JsonNode jobMetadata = readMetadata(job);
        int sceneCount = Math.max(1, jobMetadata.path("sceneCount").asInt(1));
        List<ProviderFile> scenes = new ArrayList<>();
        List<String> taskIds = new ArrayList<>();
        Map<String, Object> payload = null;
        JsonNode finalStatus = null;
        for (int scene = 1; scene <= sceneCount; scene++) {
            payload = buildPayload(job, profile, script, scene, sceneCount);
            if (sceneCount > 1) {
                payload.put("promptText", sceneDirective(scene, sceneCount) + " " + payload.get("promptText"));
            }
            String taskId = submitRender(job, payload);
            taskIds.add(taskId);
            progressCallback.onProgress(10 + (scene * 65 / sceneCount), SalesVideoStatus.VIDEO_PROCESSING,
                    "Runway aceitou cena %d/%d; taskId=%s".formatted(scene, sceneCount, taskId));
            finalStatus = waitUntilCompleted(taskId, progressCallback);
            String videoUrl = resolveVideoUrl(finalStatus);
            if (!StringUtils.hasText(videoUrl)) {
                throw new VideoProviderException("PROVIDER_RENDER_FAILED", "Runway não retornou URL da cena " + scene);
            }
            scenes.add(downloadVideo(job, videoUrl));
        }

        ProviderFile video = scenes.size() == 1 ? scenes.getFirst() : assembleScenes(job, scenes);
        String taskId = String.join(",", taskIds);
        Map<String, Object> metadata = metadata(job, taskId, payload, finalStatus, sceneCount);
        metadata.put("scene_count", sceneCount);
        metadata.put("assembled_locally", sceneCount > 1);
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Runway finalizada com MP4 disponível");
        return new ProviderArtifacts(taskId, video, null, null, metadata);
    }

    /** Define uma função narrativa distinta por cena para evitar clipes genéricos e repetitivos. */
    private String sceneDirective(int scene, int sceneCount) {
        String[] roles = {
                "DOR: mostre uma situação cotidiana reconhecível e específica, sem texto embutido.",
                "RESULTADO: mostre a transformação visual plausível e concreta, sem promessas absolutas.",
                "MECANISMO: mostre a ação prática que produz o resultado e preserve personagem e ambiente.",
                "CTA: encerre com gesto natural de decisão e espaço visual limpo para CTA em pós-produção."
        };
        int roleIndex = sceneCount == 1 ? 2 : Math.min(roles.length - 1,
                (int) Math.floor((scene - 1) * roles.length / (double) sceneCount));
        return "Cena %d de %d. FUNÇÃO COMERCIAL %s".formatted(scene, sceneCount, roles[roleIndex]);
    }

    /** Concatena as cenas Runway localmente para entregar a duração integral aprovada por Plutus. */
    private ProviderFile assembleScenes(SalesVideoJob job, List<ProviderFile> scenes) {
        List<Path> files = new ArrayList<>();
        try {
            Path manifest = Files.createTempFile("runway-scenes-" + job.id(), ".txt");
            files.add(manifest);
            StringBuilder entries = new StringBuilder();
            for (ProviderFile scene : scenes) {
                Path file = Files.createTempFile("runway-scene-" + job.id(), ".mp4");
                Files.write(file, scene.content());
                files.add(file);
                entries.append("file '").append(file.toAbsolutePath()).append("'\n");
            }
            Files.writeString(manifest, entries);
            Path output = Files.createTempFile("runway-montage-" + job.id(), ".mp4");
            files.add(output);
            Process process = new ProcessBuilder(
                    properties.getProviders().getPostProduction().getFfmpegPath(), "-y", "-f", "concat", "-safe", "0",
                    "-i", manifest.toString(), "-c", "copy", output.toString()).redirectErrorStream(true).start();
            if (process.waitFor() != 0) {
                throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "ffmpeg falhou ao montar cenas Runway");
            }
            return new ProviderFile("sales-video-" + job.id() + "-runway-montage.mp4", VIDEO_MP4,
                    AssetType.VIDEO, ProviderAssetRole.VIDEO, Files.readAllBytes(output));
        } catch (IOException ex) {
            log.error("Falha ao montar cenas Runway; jobId={}", job.id(), ex);
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Falha ao montar cenas Runway", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Montagem Runway interrompida; jobId={}", job.id(), ex);
            throw new VideoProviderException("VIDEO_ASSEMBLY_FAILED", "Montagem Runway interrompida", ex);
        } finally {
            files.forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ex) {
                    log.warn("Não foi possível remover temporário Runway {}; jobId={}", path, job.id(), ex);
                }
            });
        }
    }

    /** Cria a tarefa image-to-video ou text-to-video na Runway. */
    private String submitRender(SalesVideoJob job, Map<String, Object> payload) {
        String path = payload.containsKey("promptImage")
                ? properties.getProviders().getRunway().getCreatePath()
                : properties.getProviders().getRunway().getTextCreatePath();
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
    private Map<String, Object> buildPayload(SalesVideoJob job,
                                             SalesVideoProfile profile,
                                             SalesVideoScript script,
                                             int scene,
                                             int sceneCount) {
        VideoManagementProperties.Runway config = properties.getProviders().getRunway();
        JsonNode metadata = readMetadata(job);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModel(job, config));
        payload.put("promptText", limitPrompt(buildPrompt(job, profile, script, metadata, scene, sceneCount), 1000));
        payload.put("ratio", config.getRatio());
        payload.put("duration", resolveDuration(job, config, metadata));
        String promptImage = firstText(metadata,
                "/characterImageReferenceUrl",
                "/image_to_video/reference_image_url",
                "/image_to_video/source_image_url",
                "/promptImage");
        if (StringUtils.hasText(promptImage)) {
            payload.put("promptImage", promptImage);
        }
        if ((resolveModel(job, config).equals("gen4_turbo")
                || resolveModel(job, config).equals("grok_imagine_1_5"))
                && !StringUtils.hasText(promptImage)) {
            throw new VideoProviderException("PROVIDER_INPUT_INVALID",
                    "O modelo Runway selecionado exige uma imagem-base aprovada");
        }
        return payload;
    }

    /** Resolve o modelo pelo contrato do job, mantendo Gen-4.5 como padrão da Runway. */
    private String resolveModel(SalesVideoJob job, VideoManagementProperties.Runway config) {
        String providerName = normalize(job.providerName());
        return switch (providerName) {
            case "RUNWAY_SEEDANCE_2" -> "seedance2";
            case "RUNWAY_SEEDANCE_2_5" -> "seedance2_5";
            case "RUNWAY_HAILUO_3" -> "hailuo3";
            case "RUNWAY_GROK_IMAGINE_1_5" -> "grok_imagine_1_5";
            case "RUNWAY_GEN_4_TURBO" -> "gen4_turbo";
            case "RUNWAY_VEO_3_1" -> "veo3.1";
            case "RUNWAY_VEO_3_1_FAST" -> "veo3.1_fast";
            default -> config.getModel();
        };
    }

    /** Resolve duração compatível com o modelo selecionado antes de consumir créditos. */
    private int resolveDuration(SalesVideoJob job,
                                VideoManagementProperties.Runway config,
                                JsonNode metadata) {
        String providerName = normalize(job.providerName());
        if (providerName.equals("RUNWAY_VEO_3_1") || providerName.equals("RUNWAY_VEO_3_1_FAST")) {
            return Math.min(config.getDurationSeconds(), 8);
        }
        int planned = metadata.path("providerClipDurationSeconds").asInt(config.getDurationSeconds());
        if (providerName.contains("SEEDANCE_2")) {
            return Math.max(4, Math.min(planned, 15));
        }
        return Math.min(planned, 10);
    }

    /** Limita o prompt ao contrato oficial da Runway sem cortar um par substituto UTF-16. */
    private String limitPrompt(String prompt, int maximumUtf16Units) {
        if (prompt.length() <= maximumUtf16Units) {
            return prompt;
        }
        int end = maximumUtf16Units;
        if (Character.isHighSurrogate(prompt.charAt(end - 1))) {
            end--;
        }
        return prompt.substring(0, end).stripTrailing();
    }

    /** Monta prompt priorizando a ação da cena antes do contexto comercial sujeito ao limite da Runway. */
    private String buildPrompt(SalesVideoJob job,
                               SalesVideoProfile profile,
                               SalesVideoScript script,
                               JsonNode metadata,
                               int scene,
                               int sceneCount) {
        String visualDirectives = visualProviderDirectives(metadata);
        String scenePrompt = plannedCuts(metadata, scene, sceneCount);
        if (!StringUtils.hasText(scenePrompt)) {
            scenePrompt = metadata.path("scene").path("prompt").asText("");
        }
        String scenes = StringUtils.hasText(scenePrompt)
                ? scenePrompt
                : metadata.path("assembly_plan").path("scenes").isMissingNode()
                ? "Recognizable pain, plausible mechanism, personal value and CTA."
                : metadata.path("assembly_plan").path("scenes").toString();
        return """
                REQUIRED SCENE ACTION: %s.
                Provider-specific visual directives: %s.
                Vertical short-form sales video for a digital product.
                Language: %s.
                Title: %s.
                Audience/persona: %s.
                Communication style: %s.
                Approved hook: %s.
                Approved script context: %s.
                Approved CTA: %s.
                Scene plan: %s.
                Keep the scene natural, concrete and commercially useful. Show a human situation, the felt pain, a plausible mechanism and a light CTA.
                Do not render letters, words, captions, subtitles, UI copy, logos or watermarks in the generated footage.
                Preserve clean negative space for deterministic Portuguese copy, captions and CTA added only in post-production.
                Avoid distorted hands, haze, blur, flicker, body-focused framing, seductive posing and luxury ostentation.
                """.formatted(
                scenes,
                visualDirectives,
                nullToDefault(profile.language(), "pt-BR"),
                nullToDefault(profile.title(), "Sales video"),
                nullToDefault(profile.personaName(), "target customer"),
                nullToDefault(profile.personaStyle(), "natural and direct"),
                nullToDefault(script.hookText(), ""),
                script.scriptText(),
                nullToDefault(script.ctaText(), ""),
                scenes);
    }

    /** Seleciona apenas os cortes pertencentes ao clipe atual para evitar repetição e improviso. */
    private String plannedCuts(JsonNode metadata, int scene, int sceneCount) {
        JsonNode cuts = metadata.path("cut_plan");
        if (!cuts.isArray() || cuts.isEmpty()) {
            return "";
        }
        int start = (scene - 1) * cuts.size() / sceneCount;
        int end = Math.max(start + 1, scene * cuts.size() / sceneCount);
        List<String> selected = new ArrayList<>();
        for (int index = start; index < Math.min(end, cuts.size()); index++) {
            JsonNode cut = cuts.get(index);
            selected.add("Corte %d (%ds, %s): %s".formatted(
                    cut.path("order").asInt(index + 1),
                    cut.path("duration_seconds").asInt(3),
                    cut.path("role").asText("MECANISMO"),
                    cut.path("visual_objective").asText("ação visual única")));
        }
        return String.join(" ", selected);
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
                                         JsonNode finalStatus,
                                         int sceneCount) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "RUNWAY");
        metadata.put("provider_job_id", taskId);
        String model = String.valueOf(request.get("model"));
        int clipDurationSeconds = ((Number) request.get("duration")).intValue();
        metadata.put("model", model);
        metadata.put("ratio", properties.getProviders().getRunway().getRatio());
        metadata.put("duration_seconds", clipDurationSeconds * sceneCount);
        metadata.put("clip_duration_seconds", clipDurationSeconds);
        metadata.put("cost_usd", estimateCostUsd(model, clipDurationSeconds, sceneCount));
        metadata.put("cost_scope", "ALL_SCENES");
        metadata.put("pricing_source", "Runway API charges credits per second by model; see official pricing");
        metadata.put("request", request);
        metadata.put("final_status", objectMapper.convertValue(finalStatus, Map.class));
        metadata.put("polled_at", Instant.now().toString());
        metadata.put("source_job_id", job.id());
        return metadata;
    }

    /** Calcula custo aproximado para Gen-4.5 com créditos de US$0,01. */
    private BigDecimal estimateCostUsd(String model, int clipDurationSeconds, int sceneCount) {
        int seconds = Math.max(1, clipDurationSeconds) * Math.max(1, sceneCount);
        BigDecimal creditsPerSecond = "gen4_turbo".equalsIgnoreCase(model)
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
        String code = ex.getStatusCode().value() == 402 || insufficientCredits(body)
                ? "PROVIDER_CREDITS_INSUFFICIENT"
                : ex.getStatusCode().value() == 429 ? "PROVIDER_RATE_LIMIT" : "PROVIDER_RENDER_FAILED";
        return new VideoProviderException(code,
                "Runway retornou HTTP %d em %s: %s"
                        .formatted(ex.getStatusCode().value(), operation, body),
                ex);
    }

    /** Classifica rejeição financeira da Runway para bloquear reconciliação sem depender da mensagem. */
    private boolean insufficientCredits(String body) {
        String normalized = body == null ? "" : body.toLowerCase(Locale.ROOT);
        return normalized.contains("not enough credits")
                || normalized.contains("insufficient credits");
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
