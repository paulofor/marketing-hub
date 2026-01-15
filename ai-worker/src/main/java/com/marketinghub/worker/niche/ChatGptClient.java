package com.marketinghub.worker.niche;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptDomains;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.prompt.service.PromptService;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import com.marketinghub.worker.prompt.NichePromptContext;
import com.marketinghub.worker.prompt.PromptTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Simple wrapper around the OpenAI chat completions API.
 */
@Component
public class ChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;
    private final PromptAttributeRepository attributeRepository;
    private final PromptAttributeDescriptionRepository descriptionRepository;
    private final PromptService promptService;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(ChatGptClient.class);
    private static final String DOMAIN = PromptDomains.NICHE_HYPOTHESIS;
    private static final List<String> DEFAULT_ATTRIBUTES = List.of(
            "title",
            "promise",
            "problem",
            "persona",
            "mechanism",
            "uniqueMechanism",
            "entrega",
            "successRule",
            "offerType",
            "price"
    );
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final String COMPLETION_WINDOW = "24h";
    private static final Duration BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration BATCH_TIMEOUT = Duration.ofMinutes(2);
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    public ChatGptClient(WebClient.Builder builder,
                         ObjectMapper objectMapper,
                         @Value("${openai.api-key:}") String apiKey,
                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                         @Value("${openai.model:gpt-3.5-turbo}") String defaultModel,
                         PromptAttributeRepository attributeRepository,
                         PromptAttributeDescriptionRepository descriptionRepository,
                         PromptService promptService,
                         PromptTemplateRenderer promptTemplateRenderer,
                         AiGenerationRecorder generationRecorder) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("OpenAI-Beta", "reasoning=1");
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.defaultModel = defaultModel;
        this.attributeRepository = attributeRepository;
        this.descriptionRepository = descriptionRepository;
        this.promptService = promptService;
        this.promptTemplateRenderer = promptTemplateRenderer;
        this.generationRecorder = generationRecorder;
    }

    public List<CreateHypothesisRequest> generateHypotheses(MarketNiche niche, int quantity) {
        return generateHypotheses(niche, quantity, null);
    }

    public List<CreateHypothesisRequest> generateHypotheses(MarketNiche niche, int quantity, String model) {
        Map<Long, List<CreateHypothesisRequest>> map = generateHypothesesBatch(List.of(new HypothesisBatchRequest(niche, quantity, model)));
        return map.getOrDefault(niche.getId(), List.of());
    }

    /**
     * Generates hypotheses for the provided niches using the OpenAI Batch API.
     */
    public Map<Long, List<CreateHypothesisRequest>> generateHypothesesBatch(List<HypothesisBatchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }

        Map<String, RequestContext> contexts = new LinkedHashMap<>();
        for (HypothesisBatchRequest request : requests) {
            if (request == null || request.niche() == null) {
                continue;
            }
            int quantity = Math.max(0, request.quantity());
            if (quantity == 0) {
                continue;
            }
            String requestModel = resolveModel(request.model());
            PromptData promptData = buildPrompt(request.niche(), quantity);
            List<Map<String, Object>> input = List.of(
                    OpenAiRequestUtils.message("system", "Você é um especialista em marketing."),
                    OpenAiRequestUtils.message("user", promptData.prompt())
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", requestModel);
            payload.put("input", input);
            OpenAiRequestUtils.maybeAddReasoning(payload, requestModel);
            String customId = customId(request.niche());
            log.info("Queued batch request {} for niche {}", customId, request.niche().getId());
            contexts.put(customId, new RequestContext(request.niche(), promptData, payload, requestModel));
        }

        if (contexts.isEmpty()) {
            return Map.of();
        }

        Map<String, OpenAiResponse> responses = executeBatchRequests(contexts);
        Map<Long, List<CreateHypothesisRequest>> result = new LinkedHashMap<>();

        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            String customId = entry.getKey();
            RequestContext ctx = entry.getValue();
            OpenAiResponse response = responses.get(customId);
            if (response == null) {
                log.warn("ChatGPT batch returned no response for niche {}", ctx.niche().getId());
                continue;
            }
            if (response.hasError()) {
                throw new RuntimeException("OpenAI error: " + response.errorMessage());
            }
            String content = response.firstText();
            generationRecorder.record(DOMAIN,
                    ctx.niche() != null ? String.valueOf(ctx.niche().getId()) : null,
                    ctx.prompt().prompt(),
                    content,
                    ctx.model(),
                    response.usage());
            BigDecimal totalCost = OpenAiCostEstimator.estimateUsd(ctx.model(), response.usage());
            if (content == null || content.isBlank()) {
                log.warn("ChatGPT returned empty content for niche {}", ctx.niche().getId());
                continue;
            }
            log.info("ChatGPT content for niche {}: {}", ctx.niche().getId(), content);
            try {
                List<CreateHypothesisRequest> parsed = parseContent(content, ctx.niche(), ctx.prompt(), ctx.model(), totalCost);
                log.info("Parsed hypotheses for niche {}: {}", ctx.niche().getId(), parsed);
                result.put(ctx.niche().getId(), parsed);
            } catch (Exception e) {
                log.error("Failed to parse ChatGPT response for niche {}: {}", ctx.niche().getId(), content, e);
                try {
                    String unescaped = content.replace("\\\"", "\"");
                    List<CreateHypothesisRequest> parsed = parseContent(unescaped, ctx.niche(), ctx.prompt(), ctx.model(), totalCost);
                    log.info("Parsed hypotheses after unescaping for niche {}: {}", ctx.niche().getId(), parsed);
                    result.put(ctx.niche().getId(), parsed);
                } catch (Exception ex) {
                    log.error("Failed to parse unescaped ChatGPT response for niche {}: {}", ctx.niche().getId(), content, ex);
                    throw new RuntimeException("Failed to parse ChatGPT response", ex);
                }
            }
        }

        return result;
    }

    private PromptData buildPrompt(MarketNiche niche, int quantity) {
        Prompt promptTemplate = promptService.getActiveByDomainOrThrow(DOMAIN);
        PromptRenderContext context = buildPromptContext(niche, quantity);
        log.info("Building hypothesis prompt. promptId={}, nicheId={}, quantity={}, attributes={}",
                promptTemplate.getId(), niche != null ? niche.getId() : null, quantity, context.attributeNames());
        String rendered = promptTemplateRenderer.render(promptTemplate.getTemplate(), context.context());
        log.info("Rendered hypothesis prompt {} for niche {} ({} chars)", promptTemplate.getId(),
                niche != null ? niche.getId() : null, rendered.length());
        return new PromptData(rendered, context.descriptionIds());
    }

    private PromptRenderContext buildPromptContext(MarketNiche niche, int quantity) {
        Map<String, Object> context = new HashMap<>();
        context.put("quantity", quantity);
        context.put("niche", Optional.ofNullable(NichePromptContext.from(niche)).map(NichePromptContext::asMap).orElse(null));

        List<Long> descriptionIds = new ArrayList<>();
        List<Map<String, Object>> attributes = new ArrayList<>();
        List<PromptAttribute> attrs = attributeRepository.findByEntity_Name("hypothesis");
        for (PromptAttribute attr : attrs) {
            Optional<PromptAttributeDescription> opt = descriptionRepository.findByAttribute_IdAndActiveTrue(attr.getId());
            if (opt.isEmpty()) {
                continue;
            }
            PromptAttributeDescription desc = opt.get();
            descriptionIds.add(desc.getId());
            Map<String, Object> attrMap = new LinkedHashMap<>();
            attrMap.put("id", desc.getId());
            attrMap.put("name", attr.getName());
            attrMap.put("description", desc.getDescription());
            attributes.add(attrMap);
        }
        List<String> attributeNames = attributes.stream()
                .map(attr -> attr.get("name"))
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .toList();

        context.put("attributes", attributes);
        context.put("attributeNames", attributeNames);
        context.put("defaultAttributes", DEFAULT_ATTRIBUTES);
        return new PromptRenderContext(context, descriptionIds, attributeNames);
    }

    private List<CreateHypothesisRequest> parseContent(String content, MarketNiche niche, PromptData data, String model, BigDecimal totalCost) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        if (root.isArray()) {
            for (JsonNode node : root) {
                JsonNode entregaNode = node.get("entrega");
                if (entregaNode != null && entregaNode.isArray()) {
                    String joined = StreamSupport.stream(entregaNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .collect(Collectors.joining("\n"));
                    ((ObjectNode) node).put("entrega", joined);
                }
            }
        }
        CreateHypothesisRequest[] arr = objectMapper.treeToValue(root, CreateHypothesisRequest[].class);
        BigDecimal costPerHypothesis = null;
        if (totalCost != null && arr.length > 0) {
            costPerHypothesis = totalCost.divide(BigDecimal.valueOf(arr.length), 4, RoundingMode.HALF_UP);
        }
        for (CreateHypothesisRequest req : arr) {
            req.setMarketNicheId(niche.getId());
            req.setPrompt(data.prompt());
            req.setModel(model);
            req.setCostUsd(costPerHypothesis);
            req.setPromptAttributeDescriptionIds(data.descriptionIds());
        }
        return Arrays.asList(arr);
    }

    private Map<String, OpenAiResponse> executeBatchRequests(Map<String, RequestContext> contexts) {
        String inputFileId = uploadBatchFile(contexts);
        OpenAiBatch batch = createBatch(inputFileId);
        OpenAiBatch completed = awaitCompletion(batch);
        String outputFileId = completed.outputFileId();
        if (outputFileId == null || outputFileId.isBlank()) {
            throw new IllegalStateException("OpenAI batch did not return output_file_id");
        }
        String content = downloadFile(outputFileId);
        return parseBatchOutput(content);
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
                return "niche-hypotheses.jsonl";
            }
        };
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);
        log.info("Uploading {} requests to OpenAI batch file", contexts.size());
        OpenAiFile file = webClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .bodyToMono(OpenAiFile.class)
                .block();
        if (file == null || file.id() == null || file.id().isBlank()) {
            throw new IllegalStateException("OpenAI file upload failed for batch");
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
            throw new IllegalStateException("OpenAI batch creation failed");
        }
        return batch;
    }

    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant start = Instant.now();
        while (!isTerminal(current)) {
            if (Duration.between(start, Instant.now()).compareTo(BATCH_TIMEOUT) > 0) {
                throw new IllegalStateException("Timed out waiting for OpenAI batch " + current.id() + " in status " + current.status());
            }
            try {
                Thread.sleep(BATCH_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for batch completion", e);
            }
            current = webClient.get()
                    .uri("/batches/{id}", current.id())
                    .retrieve()
                    .bodyToMono(OpenAiBatch.class)
                    .block();
            if (current == null) {
                throw new IllegalStateException("OpenAI returned null batch while polling");
            }
        }
        if (!"completed".equals(current.status())) {
            throw new RuntimeException("OpenAI batch " + current.id() + " finished with status " + current.status());
        }
        return current;
    }

    private boolean isTerminal(OpenAiBatch batch) {
        if (batch == null) {
            return true;
        }
        String status = batch.status();
        if (status == null) {
            return false;
        }
        return TERMINAL_BATCH_STATUSES.contains(status);
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
        if (content == null || content.isBlank()) {
            return responses;
        }
        for (String line : content.split("\n")) {
            if (line.isBlank()) {
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

    private String resolveModel(String requestedModel) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }
        if (defaultModel != null && !defaultModel.isBlank()) {
            return defaultModel;
        }
        return "gpt-3.5-turbo";
    }

    private static String customId(MarketNiche niche) {
        return "niche-" + niche.getId();
    }

    private record PromptRenderContext(Map<String, Object> context, List<Long> descriptionIds, List<String> attributeNames) {}

    private record PromptData(String prompt, List<Long> descriptionIds) {}

    public record HypothesisBatchRequest(MarketNiche niche, int quantity, String model) {}

    private record RequestContext(MarketNiche niche, PromptData prompt, Map<String, Object> payload, String model) {}

    private record OpenAiFile(String id) {}

    private record OpenAiBatch(String id,
                               String status,
                               @com.fasterxml.jackson.annotation.JsonProperty("output_file_id") String outputFileId) {}

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
}
