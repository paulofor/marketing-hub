package com.marketinghub.worker.targeting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import com.marketinghub.worker.prompt.NichePromptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Responsabilidade: gerar e filtrar públicos de Meta Ads com OpenAI antes de persistir candidatos do nicho.
 */
@Component
public class TargetingElementChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(TargetingElementChatGptClient.class);
    private static final String DOMAIN = "TARGETING_ELEMENT";
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final String COMPLETION_WINDOW = "24h";
    private static final String SERVICE_TIER_FLEX = "flex";
    private static final String PROMPT_PATH = "prompts/targetingelement/targeting-element-v1.md";
    private static final Duration DEFAULT_BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration DEFAULT_BATCH_TIMEOUT = Duration.ofMinutes(5);
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiGenerationRecorder generationRecorder;
    private final String defaultModel;
    private final Duration batchPollInterval;
    private final Duration batchTimeout;

    /** Inicializa o cliente OpenAI de geração de públicos com auditoria e configuração de batch. */
    public TargetingElementChatGptClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         AiGenerationRecorder generationRecorder,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.model:gpt-5.5}") String defaultModel,
                                         @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval,
                                         @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("OpenAI-Beta", "reasoning=1")
                .build();
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.defaultModel = defaultModel;
        this.batchPollInterval = normalizeDuration(batchPollInterval, DEFAULT_BATCH_POLL_INTERVAL);
        this.batchTimeout = normalizeDuration(batchTimeout, DEFAULT_BATCH_TIMEOUT);
    }

    public record TargetingBatchRequest(MarketNiche niche, TargetingElementType type, int quantity, String model) {}

    /** Gera lotes de públicos com o modelo e mantém somente candidatos compatíveis com o nicho. */
    public Map<Long, List<CreateTargetingElementRequest>> generateBatch(List<TargetingBatchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }

        Map<String, RequestContext> contexts = new LinkedHashMap<>();
        for (TargetingBatchRequest request : requests) {
            if (request == null || request.niche() == null || request.type() == null) {
                continue;
            }
            int quantity = Math.max(0, request.quantity());
            if (quantity == 0) {
                continue;
            }
            String model = resolveModel(request.model());
            PromptData promptData = buildPrompt(request.niche(), request.type(), quantity);
            List<Map<String, Object>> input = List.of(
                    OpenAiRequestUtils.message("system", "Você é um especialista em mídia paga para Meta Ads."),
                    OpenAiRequestUtils.message("user", promptData.prompt())
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", input);
            payload.put("service_tier", SERVICE_TIER_FLEX);
            OpenAiRequestUtils.maybeAddReasoning(payload, model);

            String customId = customId(request.niche(), request.type());
            log.info("Queued targeting batch {} for niche {} and type {}", customId, request.niche().getId(), request.type());
            contexts.put(customId, new RequestContext(request, promptData, payload, model));
        }

        if (contexts.isEmpty()) {
            return Map.of();
        }

        Map<String, OpenAiResponse> responses = executeBatchRequests(contexts);
        Map<Long, List<CreateTargetingElementRequest>> result = new LinkedHashMap<>();

        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            String customId = entry.getKey();
            RequestContext ctx = entry.getValue();
            OpenAiResponse response = responses.get(customId);
            if (response == null) {
                log.warn("OpenAI batch returned no response for niche {} and type {}", ctx.request().niche().getId(), ctx.request().type());
                continue;
            }
            if (response.hasError()) {
                throw new RuntimeException("OpenAI error: " + response.errorMessage());
            }
            String content = response.firstText();
            generationRecorder.record(DOMAIN,
                    ctx.request().niche() != null ? String.valueOf(ctx.request().niche().getId()) : null,
                    ctx.prompt().prompt(),
                    content,
                    ctx.model(),
                    response.usage());
            try {
                List<CreateTargetingElementRequest> parsed = parseContent(content, ctx, response.usage());
                result.computeIfAbsent(ctx.request().niche().getId(), key -> new ArrayList<>()).addAll(parsed);
            } catch (Exception e) {
                log.error("Failed to parse ChatGPT response for niche {} and type {}: {}", ctx.request().niche().getId(), ctx.request().type(), content, e);
                try {
                    String unescaped = content.replace("\\\"", "\"");
                    List<CreateTargetingElementRequest> parsed = parseContent(unescaped, ctx, response.usage());
                    result.computeIfAbsent(ctx.request().niche().getId(), key -> new ArrayList<>()).addAll(parsed);
                } catch (Exception ex) {
                    log.error("Failed to parse unescaped ChatGPT response for niche {} and type {}: {}", ctx.request().niche().getId(), ctx.request().type(), content, ex);
                    throw new RuntimeException("Failed to parse ChatGPT response", ex);
                }
            }
        }

        return result;
    }

    /** Monta o prompt versionado com contexto do nicho e critérios de curadoria comercial. */
    private PromptData buildPrompt(MarketNiche niche, TargetingElementType type, int quantity) {
        NichePromptContext context = NichePromptContext.from(niche);
        String nicheContext = "";
        if (context != null) {
            StringBuilder sb = new StringBuilder();
            Map<String, Object> map = new HashMap<>(context.asMap());
            map.forEach((key, value) -> {
                if (value != null && StringUtils.hasText(value.toString())) {
                    sb.append("- ").append(key).append(": ").append(value).append("\n");
                }
            });
            nicheContext = sb.toString();
        }
        String prompt = loadPromptTemplate()
                .replace("{{quantity}}", String.valueOf(quantity))
                .replace("{{typeLabel}}", typeLabel(type))
                .replace("{{nicheContext}}", nicheContext);
        return new PromptData(prompt);
    }

    /** Lê o prompt operacional do classpath para evitar instruções comerciais hardcoded. */
    private String loadPromptTemplate() {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar prompt de curadoria de públicos no caminho {}", PROMPT_PATH, ex);
            throw new IllegalStateException("Não foi possível carregar o prompt de curadoria de públicos", ex);
        }
    }

    /** Interpreta a resposta do modelo e descarta candidatos que o próprio modelo marcou como fracos. */
    private List<CreateTargetingElementRequest> parseContent(String content, RequestContext context, OpenAiResponse.OpenAiUsage usage) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        JsonNode itemsNode = extractItemsNode(root, context.request().type());
        List<JsonNode> nodes = normalizeToList(itemsNode);
        if (nodes.isEmpty() && root != null && root.isArray()) {
            nodes = normalizeToList(root);
        }

        List<CreateTargetingElementRequest> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode node : nodes) {
            String term = extractTerm(node);
            if (!StringUtils.hasText(term)) {
                continue;
            }
            String normalizedKey = (context.request().type().name() + "|" + term).toLowerCase();
            if (!seen.add(normalizedKey)) {
                continue;
            }
            java.math.BigDecimal confidence = extractConfidence(node);
            if (confidence != null && confidence.compareTo(java.math.BigDecimal.valueOf(0.75)) < 0) {
                log.info(
                        "Descartando público {} para nicho {} por baixa aderência de IA: {}",
                        term,
                        context.request().niche().getId(),
                        confidence);
                continue;
            }
            CreateTargetingElementRequest req = new CreateTargetingElementRequest();
            req.setMarketNicheId(context.request().niche().getId());
            req.setType(context.request().type());
            req.setTerm(term.trim());
            req.setDescription(extractDescription(node));
            req.setPrompt(context.prompt().prompt());
            req.setModel(context.model());
            req.setSource(TargetingElementSource.AI);
            req.setStatus(TargetingElementStatus.NEEDS_REVIEW);
            req.setConfidence(confidence);
            req.setNotes(extractNotes(node));
            req.setMetaId(extractText(node, "metaId", null));
            req.setMetaKey(extractText(node, "metaKey", null));
            list.add(req);
        }
        return list;
    }

    private JsonNode extractItemsNode(JsonNode root, TargetingElementType type) {
        if (root == null || root.isNull()) return null;
        if (root.isArray()) return root;
        JsonNode items = root.get("items");
        if (items == null || items.isNull()) {
            String field = switch (type) {
                case INTEREST -> "interests";
                case JOB_TITLE -> "jobTitles";
                case BEHAVIOR -> "behaviors";
            };
            items = root.get(field);
        }
        return items != null ? items : root;
    }

    private List<JsonNode> normalizeToList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false).toList();
        }
        if (node.isObject()) {
            return List.of(node);
        }
        ArrayNode wrapper = objectMapper.createArrayNode();
        wrapper.add(node);
        return StreamSupport.stream(wrapper.spliterator(), false).toList();
    }

    private String extractTerm(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String term = extractText(node, "term", null);
        if (!StringUtils.hasText(term)) {
            term = extractText(node, "name", null);
        }
        return term;
    }

    private String extractDescription(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String description = extractText(node, "description", null);
        if (!StringUtils.hasText(description)) {
            description = extractText(node, "why", null);
        }
        if (!StringUtils.hasText(description)) {
            description = extractText(node, "rationale", null);
        }
        return description;
    }

    private java.math.BigDecimal extractConfidence(JsonNode node) {
        if (node == null || node.isNull()) return null;
        JsonNode confidence = node.get("confidence");
        if (confidence != null && confidence.isNumber()) {
            return confidence.decimalValue();
        }
        return null;
    }

    private String extractNotes(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String notes = extractText(node, "notes", null);
        if (!StringUtils.hasText(notes)) {
            notes = extractText(node, "observation", null);
        }
        return notes;
    }

    private String extractText(JsonNode node, String field, String fallback) {
        if (node == null || node.isNull()) return fallback;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return fallback;
        if (value.isArray()) {
            return StreamSupport.stream(value.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : fallback;
    }

    private String typeLabel(TargetingElementType type) {
        return switch (type) {
            case INTEREST -> "interesses (adinterest)";
            case JOB_TITLE -> "cargos (work_positions)";
            case BEHAVIOR -> "comportamentos (adTargetingCategory)";
        };
    }

    private String resolveModel(String model) {
        if (!StringUtils.hasText(model)) {
            if (StringUtils.hasText(defaultModel)) {
                return defaultModel;
            }
            return "gpt-5.5";
        }
        return model;
    }

    private String customId(MarketNiche niche, TargetingElementType type) {
        return "niche-" + (niche != null ? niche.getId() : "unknown") + "-" + type.name().toLowerCase();
    }

    private Map<String, OpenAiResponse> executeBatchRequests(Map<String, RequestContext> contexts) {
        String inputFileId = uploadBatchFile(contexts);
        OpenAiBatch batch = createBatch(inputFileId);
        OpenAiBatch completed = awaitCompletion(batch);
        String outputFileId = completed.outputFileId();
        if (!StringUtils.hasText(outputFileId)) {
            throw new IllegalStateException("OpenAI batch did not return output_file_id");
        }
        String fileContent = downloadFile(outputFileId);
        return parseBatchOutput(fileContent);
    }

    private String uploadBatchFile(Map<String, RequestContext> contexts) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("custom_id", entry.getKey());
            line.put("method", "POST");
            line.put("url", RESPONSES_ENDPOINT);
            line.put("body", entry.getValue().payload());
            try {
                sb.append(objectMapper.writeValueAsString(line)).append("\n");
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize batch line for " + entry.getKey(), e);
            }
        }
        byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return "targeting-elements.jsonl";
            }
        };
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);
        log.info("Uploading {} targeting requests to OpenAI batch file", contexts.size());
        OpenAiFile file = webClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .bodyToMono(OpenAiFile.class)
                .block();
        if (file == null || !StringUtils.hasText(file.id())) {
            throw new IllegalStateException("OpenAI file upload failed for targeting batch");
        }
        return file.id();
    }

    private OpenAiBatch createBatch(String inputFileId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("input_file_id", inputFileId);
        payload.put("endpoint", RESPONSES_ENDPOINT);
        payload.put("completion_window", COMPLETION_WINDOW);
        OpenAiBatch batch = webClient.post()
                .uri("/batches")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiBatch.class)
                .block();
        if (batch == null || batch.id() == null) {
            throw new IllegalStateException("OpenAI batch creation failed for targeting elements");
        }
        return batch;
    }

    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant start = Instant.now();
        while (!isTerminal(current)) {
            if (Duration.between(start, Instant.now()).compareTo(batchTimeout) > 0) {
                throw new IllegalStateException("Timed out waiting for OpenAI batch " + current.id() + " after " + batchTimeout);
            }
            try {
                Thread.sleep(batchPollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for OpenAI batch", e);
            }
            current = webClient.get()
                    .uri("/batches/{id}", current.id())
                    .retrieve()
                    .bodyToMono(OpenAiBatch.class)
                    .block();
        }
        if (!"completed".equalsIgnoreCase(current.status())) {
            throw new IllegalStateException("OpenAI batch ended with status " + current.status());
        }
        return current;
    }

    private boolean isTerminal(OpenAiBatch batch) {
        if (batch == null || batch.status() == null) return true;
        return TERMINAL_BATCH_STATUSES.contains(batch.status());
    }

    private String downloadFile(String fileId) {
        return webClient.get()
                .uri("/files/{id}/content", fileId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Map<String, OpenAiResponse> parseBatchOutput(String content) {
        Map<String, OpenAiResponse> responses = new LinkedHashMap<>();
        if (!StringUtils.hasText(content)) {
            return responses;
        }
        for (String line : content.split("\n")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            try {
                BatchOutput output = objectMapper.readValue(line, BatchOutput.class);
                if (output.response() != null && output.response().isSuccessful()) {
                    Map<String, Object> body = output.response().body();
                    if (body == null) {
                        log.warn("Skipping batch line {} because body is null", output.customId());
                        continue;
                    }
                    OpenAiResponse response = objectMapper.convertValue(body, OpenAiResponse.class);
                    responses.put(output.customId(), response);
                } else if (output.response() != null) {
                    log.error("OpenAI batch request {} failed with status {}", output.customId(), output.response().statusCode());
                } else if (output.error() != null) {
                    log.error("OpenAI batch request {} failed: {} - {}", output.customId(), output.error().code(), output.error().message());
                }
            } catch (Exception e) {
                log.error("Failed to parse batch output line: {}", line, e);
            }
        }
        return responses;
    }

    private record BatchOutput(@com.fasterxml.jackson.annotation.JsonProperty("custom_id") String customId,
                               BatchResponse response,
                               BatchError error) {}

    private record BatchResponse(@com.fasterxml.jackson.annotation.JsonProperty("status_code") Integer statusCode,
                                 @com.fasterxml.jackson.annotation.JsonProperty("request_id") String requestId,
                                 Map<String, Object> body) {
        boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private record BatchError(String code, String message, String param, String line) {}

    private record RequestContext(TargetingBatchRequest request,
                                  PromptData prompt,
                                  Map<String, Object> payload,
                                  String model) {}

    private record PromptData(String prompt) {}

    private record OpenAiBatch(String id, String status,
                               @com.fasterxml.jackson.annotation.JsonProperty("output_file_id") String outputFileId) {}

    private record OpenAiFile(String id) {}

    private Duration normalizeDuration(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}

