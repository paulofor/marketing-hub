package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageBackendPort;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: integrar a etapa imagegeneration do core OpenAI aos endpoints novos do GeraLanding. */
public class ImageGenerationBackendClient implements StageBackendPort<ImageGenerationInput, ImageGenerationOutput> {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationBackendClient.class);
    private static final String STATUS_STARTED = "INICIADO";

    private final FrameworkImageStorageClient storageClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ImageGenerationWorkerProperties properties;
    private final String workerId;

    /** Inicializa o adapter com storage, otimização, cliente HTTP e propriedades da etapa GeraLanding imagegeneration. */
    public ImageGenerationBackendClient(
            FrameworkImageStorageClient storageClient,
            CreativeImageOptimizer imageOptimizer,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            ImageGenerationWorkerProperties properties
    ) {
        this.storageClient = storageClient;
        this.imageOptimizer = imageOptimizer;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.workerId = resolveWorkerId(properties.workerId());
    }

    /** Busca jobs pendentes no endpoint novo do GeraLanding e monta execuções para o worker genérico. */
    @Override
    public List<StageExecution<ImageGenerationInput>> listPending(int limit) {
        if (properties.rolloutPercentage() <= 0) {
            log.debug("ImageGeneration worker rollout disabled (rolloutPercentage={})", properties.rolloutPercentage());
            return List.of();
        }

        int effectiveLimit = Math.max(1, limit);
        List<Map<String, Object>> payload = webClient.get()
                .uri(stageExecutionBaseUrl() + "/pending")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block(properties.timeout());
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        return payload.stream()
                .map(this::toStageExecution)
                .filter(execution -> execution.aggregateId() != null && execution.idJob() != null)
                .filter(execution -> isRolloutEligible(execution.aggregateId()))
                .limit(effectiveLimit)
                .toList();
    }

    /** Envia ao backend GeraLanding o prompt consolidado, schema e request cru despachados para a OpenAI. */
    @Override
    public void markDispatched(StageExecution<ImageGenerationInput> execution, OpenAiDispatch dispatch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", dispatch.prompt());
        body.put("promptMarkdownContent", dispatch.promptMarkdownContent());
        body.put("schemaJson", dispatch.schemaJson());
        body.put("requestBodyJson", dispatch.requestBodyJson());
        body.put("jobidopenai", dispatch.openAiJobId());

        log.info(
                "Envio para backend GeraLanding após despacho imagegeneration [jobId={}, experimentId={}, payload={}]",
                execution.idJob(),
                execution.aggregateId(),
                body
        );
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-prompt", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Publica as imagens validadas no storage e conclui o job GeraLanding com manifesto contratual. */
    @Override
    public void markCompleted(StageExecution<ImageGenerationInput> execution, OpenAiResult<ImageGenerationOutput> result) {
        List<Map<String, Object>> manifestImages = new ArrayList<>();
        for (ImageGenerationOutput.GeneratedImage generated : result.parsedResponse().images()) {
            byte[] imageContent = resolveImageContent(generated, execution.idJob());
            CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(imageContent);
            FrameworkImageStorageClient.UploadedFrameworkImage uploaded = uploadWithRetry(
                    optimized.content(),
                    buildFilename(execution.idJob(), generated)
            );
            manifestImages.add(toManifestImage(generated, result.openAiJobId(), uploaded));
        }

        String manifest = buildManifest(execution, result, manifestImages);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", manifest);
        body.put("inputTokens", result.inputTokens());
        body.put("outputTokens", result.outputTokens());
        body.put("costUsd", result.costUsd());
        body.put("openAiJobId", result.openAiJobId());
        body.put("errorMessage", null);
        body.put("errorDetail", null);

        log.info(
                "Envio para backend GeraLanding concluindo imagegeneration [jobId={}, experimentId={}, payload={}]",
                execution.idJob(),
                execution.aggregateId(),
                body
        );
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Envia ao backend GeraLanding os dados de falha quando a etapa imagegeneration não é concluída. */
    @Override
    public void markFailed(StageExecution<ImageGenerationInput> execution, Throwable error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", execution.aggregateId());
        body.put("stageCode", execution.stageCode());
        body.put("modelResponse", null);
        body.put("inputTokens", null);
        body.put("outputTokens", null);
        body.put("costUsd", null);
        body.put("openAiJobId", null);
        body.put("errorMessage", buildFailureReason(error));
        body.put("errorDetail", error != null ? stackTraceSummary(error) : null);

        log.info(
                "Envio para backend GeraLanding falhando imagegeneration [jobId={}, experimentId={}, payload={}]",
                execution.idJob(),
                execution.aggregateId(),
                body
        );
        webClient.post()
                .uri(stageExecutionBaseUrl() + "/{idJob}/recebe-resposta", execution.idJob())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(properties.timeout());
    }

    /** Converte o payload pendente do GeraLanding para o modelo interno de execução da etapa. */
    private StageExecution<ImageGenerationInput> toStageExecution(Map<String, Object> item) {
        Long experimentId = asLong(item.get("experimentId"));
        String stageCode = asString(item.get("stageCode"));
        String idJob = asString(firstNonNull(item.get("jobid"), item.get("idJob")));
        ImageGenerationInput input = new ImageGenerationInput(
                experimentId,
                stageCode,
                idJob,
                extractImagePromptItems(item)
        );
        return new StageExecution<>(
                idJob,
                experimentId,
                stageCode,
                STATUS_STARTED,
                asInstant(item.get("executionRequestedAt")),
                input
        );
    }

    /** Extrai prompts de landingPageImagePlanning preservando sectionId, elementId e chaves de vínculo. */
    private List<ImageGenerationInput.ImageGenerationPromptItem> extractImagePromptItems(Map<String, Object> pending) {
        Map<String, Object> experiment = asMap(pending.get("experiment"));
        Object normalizedPlanning = normalizeJsonArtifact(experiment.get("landingPageImagePlanning"));
        Map<String, Object> planningRoot = asMap(normalizedPlanning);
        Object rawPlanning = firstNonNull(planningRoot.get("landingPageImagePlanning"), planningRoot);
        Map<String, Object> planning = asMap(rawPlanning);
        Object rawImages = planning.get("images");
        if (!(rawImages instanceof List<?> images)) {
            return List.of();
        }

        List<ImageGenerationInput.ImageGenerationPromptItem> result = new ArrayList<>();
        for (Object rawImage : images) {
            Map<String, Object> image = asMap(rawImage);
            String prompt = asString(firstNonNull(image.get("imagePrompt"), image.get("prompt")));
            if (!StringUtils.hasText(prompt)) {
                continue;
            }
            result.add(new ImageGenerationInput.ImageGenerationPromptItem(
                    asString(firstNonNull(image.get("planningItemKey"), image.get("imageBindingKey"), image.get("elementId"), image.get("sectionId"))),
                    asString(image.get("sectionId")),
                    asString(image.get("elementId")),
                    asString(image.get("imageGoal")),
                    prompt
            ));
        }
        return result;
    }

    /** Normaliza artefatos que podem chegar como JSON textual, objeto estruturado ou valor simples. */
    private Object normalizeJsonArtifact(Object value) {
        if (value instanceof String text) {
            return parseJsonField(text);
        }
        return value != null ? value : Map.of();
    }

    /** Interpreta um campo textual JSON quando possível, preservando mapa vazio em caso de ausência. */
    private Object parseJsonField(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException error) {
            log.warn("Image generation artifact is not valid JSON; preserving raw text. rawLength={}", raw.length(), error);
            return raw;
        }
    }

    /** Resolve os bytes da imagem usando o base64 retornado ou baixando a URL informada pela OpenAI. */
    private byte[] resolveImageContent(ImageGenerationOutput.GeneratedImage generated, String jobId) {
        if (generated.imageContent() != null && generated.imageContent().length > 0) {
            return generated.imageContent();
        }
        if (StringUtils.hasText(generated.imageUrl())) {
            byte[] downloaded = webClient.get()
                    .uri(generated.imageUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(properties.timeout());
            if (downloaded != null && downloaded.length > 0) {
                return downloaded;
            }
        }
        throw new IllegalStateException("OpenAI did not provide image bytes for GeraLanding job " + jobId);
    }

    /** Faz upload com retentativas simples para reduzir falhas transitórias de storage. */
    private FrameworkImageStorageClient.UploadedFrameworkImage uploadWithRetry(byte[] content, String preferredName) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= properties.uploadAttempts(); attempt++) {
            try {
                return storageClient.upload(content, preferredName);
            } catch (RuntimeException error) {
                lastError = error;
                if (attempt >= properties.uploadAttempts()) {
                    log.error(
                            "ImageGeneration upload failed definitively on attempt {}/{} for preferredName={}",
                            attempt,
                            properties.uploadAttempts(),
                            preferredName,
                            error
                    );
                    break;
                }
                long backoffMillis = properties.uploadBackoff().toMillis() * attempt;
                log.warn(
                        "ImageGeneration upload failed on attempt {}/{} for preferredName={}: {}. Retrying in {}ms",
                        attempt,
                        properties.uploadAttempts(),
                        preferredName,
                        error.getMessage(),
                        backoffMillis,
                        error
                );
                sleep(backoffMillis);
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("ImageGeneration upload failed");
    }

    /** Aguarda antes de uma nova tentativa de upload preservando interrupção da thread. */
    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(50L, millis));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            log.error("ImageGeneration upload retry sleep interrupted. millis={}", millis, error);
            throw new RuntimeException("Interrupted while waiting to retry imagegeneration upload", error);
        }
    }

    /** Monta um item do manifesto final de imagens com URLs definitivas publicáveis. */
    private Map<String, Object> toManifestImage(
            ImageGenerationOutput.GeneratedImage generated,
            String openAiJobId,
            FrameworkImageStorageClient.UploadedFrameworkImage uploaded
    ) {
        Map<String, Object> image = new LinkedHashMap<>();
        putIfPresent(image, "planningItemKey", firstText(generated.planningItemKey(), generated.elementId(), generated.sectionId()));
        putIfPresent(image, "sectionId", generated.sectionId());
        putIfPresent(image, "elementId", generated.elementId());
        putIfPresent(image, "imageGoal", generated.imageGoal());
        putIfPresent(image, "prompt", generated.prompt());
        putIfPresent(image, "model", generated.model());
        putIfPresent(image, "openAiJobId", openAiJobId);
        putIfPresent(image, "sourceUrl", uploaded.publicUrl());
        putIfPresent(image, "resolvedUrl", uploaded.publicUrl());
        image.put("status", "COMPLETED");
        return image;
    }

    /** Serializa o manifesto de imagens consumido pelas próximas etapas do GeraLanding. */
    private String buildManifest(
            StageExecution<ImageGenerationInput> execution,
            OpenAiResult<ImageGenerationOutput> result,
            List<Map<String, Object>> images
    ) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("version", 1);
            root.put("experimentId", execution.aggregateId());
            root.put("stageCode", execution.stageCode());
            root.put("idJob", execution.idJob());
            root.put("openAiJobId", result.openAiJobId());
            root.put("generatedAt", Instant.now().toString());
            root.put("images", images);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException error) {
            log.error(
                    "Erro ao serializar manifesto GeraLanding imagegeneration (jobId={}, experimentId={}, imageCount={})",
                    execution.idJob(),
                    execution.aggregateId(),
                    images.size(),
                    error
            );
            throw new IllegalStateException("Falha ao serializar manifesto GeraLanding imagegeneration", error);
        }
    }

    /** Monta um nome de arquivo estável para o item planejado da landing. */
    private String buildFilename(String jobId, ImageGenerationOutput.GeneratedImage generated) {
        String key = firstText(generated.planningItemKey(), generated.elementId(), generated.sectionId(), "item");
        String normalized = key.toLowerCase()
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");
        return jobId + "-" + normalized + ".jpg";
    }

    /** Resolve o identificador operacional deste worker para logs de rollout. */
    private String resolveWorkerId(String configuredWorkerId) {
        if (StringUtils.hasText(configuredWorkerId)) {
            return configuredWorkerId.trim();
        }
        try {
            return "imagegeneration-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception error) {
            log.warn("Could not resolve hostname for imagegeneration workerId; using fallback", error);
            return "imagegeneration-worker";
        }
    }

    /** Gera uma mensagem curta de erro adequada para persistência no backend. */
    private String buildFailureReason(Throwable error) {
        String message = error != null ? error.getMessage() : null;
        if (!StringUtils.hasText(message)) {
            return "Falha desconhecida no processamento do job de imagem";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /** Resume a stack trace em texto para envio controlado ao backend. */
    private String stackTraceSummary(Throwable error) {
        StringBuilder builder = new StringBuilder(error.toString());
        for (StackTraceElement element : error.getStackTrace()) {
            builder.append("\n    at ").append(element);
            if (builder.length() > 4000) {
                break;
            }
        }
        return builder.toString();
    }

    /** Verifica se o experimento pertence ao bucket de rollout configurado. */
    private boolean isRolloutEligible(Long experimentId) {
        if (properties.rolloutPercentage() >= 100) {
            return true;
        }
        long normalizedExperimentId = experimentId == null ? 0L : Math.abs(experimentId);
        long bucket = normalizedExperimentId % 100;
        boolean eligible = bucket < properties.rolloutPercentage();
        if (!eligible) {
            log.info(
                    "ImageGeneration rollout skipped experimentId={} workerId={} rolloutPercentage={}",
                    experimentId,
                    workerId,
                    properties.rolloutPercentage()
            );
        }
        return eligible;
    }

    /** Monta a URL base dos endpoints internos novos de execução imagegeneration do GeraLanding. */
    private String stageExecutionBaseUrl() {
        return joinPath(
                properties.backendBaseUrl(),
                properties.apiPrefix(),
                "/internal/geralanding/image-generation/stage-executions"
        );
    }

    /** Extrai um mapa tipado quando o valor recebido é um objeto JSON. */
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> converted = new LinkedHashMap<>();
            rawMap.forEach((key, rawValue) -> {
                if (key != null) {
                    converted.put(String.valueOf(key), rawValue);
                }
            });
            return converted;
        }
        return Map.of();
    }

    /** Converte um valor genérico para Long quando possível. */
    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    /** Converte um valor genérico para texto preservando nulo quando ausente. */
    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Converte um valor genérico para Instant quando possível. */
    private Instant asInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text.trim());
        }
        return null;
    }

    /** Retorna o primeiro valor não nulo entre os candidatos informados. */
    private Object firstNonNull(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /** Retorna o primeiro texto preenchido entre os candidatos informados. */
    private String firstText(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    /** Adiciona um campo textual ao payload somente quando há valor útil. */
    private void putIfPresent(Map<String, Object> payload, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            payload.put(fieldName, value.trim());
        }
    }

    /** Junta partes de URL evitando barras duplicadas entre segmentos. */
    private String joinPath(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String normalized = part.trim();
            if (builder.length() == 0) {
                builder.append(normalized.replaceAll("/+$", ""));
            } else {
                builder.append('/').append(normalized.replaceAll("^/+", "").replaceAll("/+$", ""));
            }
        }
        return builder.toString();
    }
}
