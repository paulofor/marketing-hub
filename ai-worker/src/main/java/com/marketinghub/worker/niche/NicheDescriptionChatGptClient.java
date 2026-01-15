package com.marketinghub.worker.niche;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.PromptDomains;
import com.marketinghub.prompt.service.PromptService;
import com.marketinghub.worker.prompt.NichePromptContext;
import com.marketinghub.worker.prompt.PromptTemplateRenderer;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class NicheDescriptionChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(NicheDescriptionChatGptClient.class);
    private static final String DOMAIN = PromptDomains.NICHE_DETAILED_DESCRIPTION;
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final String COMPLETION_WINDOW = "24h";
    private static final Duration BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration BATCH_TIMEOUT = Duration.ofMinutes(2);
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;
    private final AiGenerationRecorder generationRecorder;
    private final PromptService promptService;
    private final PromptTemplateRenderer promptTemplateRenderer;

    public NicheDescriptionChatGptClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         AiGenerationRecorder generationRecorder,
                                         PromptService promptService,
                                         PromptTemplateRenderer promptTemplateRenderer,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.model:gpt-3.5-turbo}") String defaultModel) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("OpenAI-Beta", "reasoning=1")
                .build();
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.promptService = promptService;
        this.promptTemplateRenderer = promptTemplateRenderer;
        this.defaultModel = defaultModel;
    }

    public record DescriptionBatchRequest(MarketNiche niche, int quantity, String model) {}

    public Map<Long, List<CreateNicheDetailedDescriptionRequest>> generateDescriptionsBatch(List<DescriptionBatchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }
        Map<String, RequestContext> contexts = new LinkedHashMap<>();
        Prompt promptTemplate = promptService.getActiveByDomainOrThrow(PromptDomains.NICHE_DETAILED_DESCRIPTION);
        for (DescriptionBatchRequest request : requests) {
            if (request == null || request.niche() == null) {
                continue;
            }
            int quantity = Math.max(0, request.quantity());
            if (quantity == 0) {
                continue;
            }
            String model = resolveModel(request.model());
            PromptData promptData = buildPrompt(promptTemplate, request.niche(), quantity);
            List<Map<String, Object>> input = List.of(
                    OpenAiRequestUtils.message("system", "Você é um especialista em marketing."),
                    OpenAiRequestUtils.message("user", promptData.prompt())
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", input);
            OpenAiRequestUtils.maybeAddReasoning(payload, model);
            String customId = customId(request.niche());
            log.info("Queued detailed description batch {} for niche {}", customId, request.niche().getId());
            contexts.put(customId, new RequestContext(request.niche(), promptData, payload, model));
        }
        if (contexts.isEmpty()) {
            return Map.of();
        }

        Map<String, OpenAiResponse> responses = executeBatchRequests(contexts);
        Map<Long, List<CreateNicheDetailedDescriptionRequest>> result = new LinkedHashMap<>();

        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            String customId = entry.getKey();
            RequestContext ctx = entry.getValue();
            OpenAiResponse response = responses.get(customId);
            if (response == null) {
                log.warn("OpenAI batch returned no response for niche {}", ctx.niche().getId());
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
            try {
                List<CreateNicheDetailedDescriptionRequest> parsed = parseContent(content, ctx.niche(), ctx.prompt(), ctx.model(), response.usage());
                result.put(ctx.niche().getId(), parsed);
            } catch (Exception e) {
                log.error("Failed to parse ChatGPT response for niche {}: {}", ctx.niche().getId(), content, e);
                try {
                    String unescaped = content.replace("\\\"", "\"");
                    List<CreateNicheDetailedDescriptionRequest> parsed = parseContent(unescaped, ctx.niche(), ctx.prompt(), ctx.model(), response.usage());
                    result.put(ctx.niche().getId(), parsed);
                } catch (Exception ex) {
                    log.error("Failed to parse unescaped ChatGPT response for niche {}: {}", ctx.niche().getId(), content, ex);
                    throw new RuntimeException("Failed to parse ChatGPT response", ex);
                }
            }
        }
        return result;
    }
    private PromptData buildPrompt(Prompt promptTemplate, MarketNiche niche, int quantity) {
        Map<String, Object> context = new HashMap<>();
        context.put("quantity", quantity);
        context.put("niche", Optional.ofNullable(NichePromptContext.from(niche)).map(NichePromptContext::asMap).orElse(null));
        log.info("Building detailed description prompt. promptId={}, nicheId={}, quantity={}, context={}",
                promptTemplate.getId(), niche != null ? niche.getId() : null, quantity, context);
        String rendered = promptTemplateRenderer.render(promptTemplate.getTemplate(), context);
        log.info("Rendered detailed description prompt {} for niche {} ({} chars)", promptTemplate.getId(), niche != null ? niche.getId() : null, rendered.length());
        return new PromptData(promptTemplate.getId(), rendered);
    }
    private List<CreateNicheDetailedDescriptionRequest> parseContent(String content, MarketNiche niche, PromptData data, String model, OpenAiResponse.OpenAiUsage usage) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        if (!root.isArray()) {
            ArrayNode wrapper = objectMapper.createArrayNode();
            wrapper.add(root);
            root = wrapper;
        }
        int totalItems = root.size();
        BigDecimal totalCost = OpenAiCostEstimator.estimateUsd(model, usage);
        BigDecimal costPerItem = totalItems > 0
                ? totalCost.divide(BigDecimal.valueOf(totalItems), 4, RoundingMode.HALF_UP)
                : totalCost;
        Integer inputTokens = usage != null ? usage.effectiveInputTokens() : null;
        Integer outputTokens = usage != null ? usage.effectiveOutputTokens() : null;
        Integer inputPerItem = inputTokens != null && totalItems > 0 ? inputTokens / totalItems : inputTokens;
        Integer outputPerItem = outputTokens != null && totalItems > 0 ? outputTokens / totalItems : outputTokens;

        List<CreateNicheDetailedDescriptionRequest> list = new ArrayList<>();
        int index = 1;
        for (JsonNode node : root) {
            CreateNicheDetailedDescriptionRequest req = new CreateNicheDetailedDescriptionRequest();
            req.setMarketNicheId(niche.getId());
            req.setTitle(extractText(node, "title", "Descrição detalhada " + index));
            req.setDescription(extractText(node, "overview", extractText(node, "description", null)));
            req.setPains(joinArray(node.get("pains")));
            req.setDesires(joinArray(node.get("desires")));
            req.setNeeds(joinArray(node.get("needs")));
            req.setPromptId(data.promptId());
            req.setPrompt(data.prompt());
            req.setModel(model);
            req.setCostUsd(costPerItem);
            req.setInputTokens(inputPerItem);
            req.setOutputTokens(outputPerItem);
            if (req.getDescription() != null && !req.getDescription().isBlank()) {
                list.add(req);
                index++;
            }
        }
        return list;
    }

    private String extractText(JsonNode node, String field, String fallback) {
        if (node == null) return fallback;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return fallback;
        if (value.isArray()) {
            return joinArray(value);
        }
        if (value.isObject()) {
            return value.toString();
        }
        String text = value.asText();
        return text == null || text.isBlank() ? fallback : text;
    }

    private String joinArray(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .map(JsonNode::asText)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("\n"));
        }
        return node.asText();
    }

    private String resolveModel(String model) {
        if (model == null || model.isBlank()) {
            if (defaultModel != null && !defaultModel.isBlank()) {
                return defaultModel;
            }
            return "gpt-3.5-turbo";
        }
        return model;
    }

    private String customId(MarketNiche niche) {
        return "niche-" + (niche != null ? niche.getId() : "unknown") + "-descriptions";
    }

    private Map<String, OpenAiResponse> executeBatchRequests(Map<String, RequestContext> contexts) {
        String inputFileId = uploadBatchFile(contexts);
        OpenAiBatch batch = createBatch(inputFileId);
        OpenAiBatch completed = awaitCompletion(batch);
        String outputFileId = completed.outputFileId();
        if (outputFileId == null || outputFileId.isBlank()) {
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
                return "niche-detailed-descriptions.jsonl";
            }
        };
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);
        log.info("Uploading {} description requests to OpenAI batch file", contexts.size());
        OpenAiFile file = webClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .bodyToMono(OpenAiFile.class)
                .block();
        if (file == null || file.id() == null || file.id().isBlank()) {
            throw new IllegalStateException("OpenAI file upload failed for description batch");
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
            throw new IllegalStateException("OpenAI batch creation failed for descriptions");
        }
        return batch;
    }

    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant start = Instant.now();
        while (!isTerminal(current)) {
            if (Duration.between(start, Instant.now()).compareTo(BATCH_TIMEOUT) > 0) {
                throw new IllegalStateException("Timed out waiting for OpenAI batch " + current.id());
            }
            try {
                Thread.sleep(BATCH_POLL_INTERVAL.toMillis());
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

    private record RequestContext(MarketNiche niche, PromptData prompt, Map<String, Object> payload, String model) {}

    private record PromptData(Long promptId, String prompt) {}

    private record OpenAiBatch(String id, String status,
                               @com.fasterxml.jackson.annotation.JsonProperty("output_file_id") String outputFileId) {}

    private record OpenAiFile(String id) {}
}
