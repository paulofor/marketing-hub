package com.marketinghub.worker.niche;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.worker.openai.AiGenerationRecorder;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final String model;
    private final PromptAttributeRepository attributeRepository;
    private final PromptAttributeDescriptionRepository descriptionRepository;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(ChatGptClient.class);
    private static final String DOMAIN = "NICHE_HYPOTHESIS";
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final String COMPLETION_WINDOW = "24h";
    private static final Duration BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration BATCH_TIMEOUT = Duration.ofMinutes(2);
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    public ChatGptClient(WebClient.Builder builder,
                         ObjectMapper objectMapper,
                         @Value("${openai.api-key:}") String apiKey,
                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                         @Value("${openai.model:gpt-3.5-turbo}") String model,
                         PromptAttributeRepository attributeRepository,
                         PromptAttributeDescriptionRepository descriptionRepository,
                         AiGenerationRecorder generationRecorder) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.attributeRepository = attributeRepository;
        this.descriptionRepository = descriptionRepository;
        this.generationRecorder = generationRecorder;
    }

    public List<CreateHypothesisRequest> generateHypotheses(MarketNiche niche, int quantity) {
        Map<Long, List<CreateHypothesisRequest>> map = generateHypothesesBatch(List.of(new HypothesisBatchRequest(niche, quantity)));
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
            PromptData promptData = buildPrompt(request.niche(), quantity);
            List<Map<String, Object>> input = List.of(
                    OpenAiRequestUtils.message("system", "Você é um especialista em marketing."),
                    OpenAiRequestUtils.message("user", promptData.prompt())
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", input);
            OpenAiRequestUtils.maybeAddReasoning(payload, model);
            String customId = customId(request.niche());
            log.info("Queued batch request {} for niche {}", customId, request.niche().getId());
            contexts.put(customId, new RequestContext(request.niche(), promptData, payload));
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
                    model,
                    response.usage());
            if (content == null || content.isBlank()) {
                log.warn("ChatGPT returned empty content for niche {}", ctx.niche().getId());
                continue;
            }
            log.info("ChatGPT content for niche {}: {}", ctx.niche().getId(), content);
            try {
                List<CreateHypothesisRequest> parsed = parseContent(content, ctx.niche(), ctx.prompt());
                log.info("Parsed hypotheses for niche {}: {}", ctx.niche().getId(), parsed);
                result.put(ctx.niche().getId(), parsed);
            } catch (Exception e) {
                log.error("Failed to parse ChatGPT response for niche {}: {}", ctx.niche().getId(), content, e);
                try {
                    String unescaped = content.replace("\\\"", "\"");
                    List<CreateHypothesisRequest> parsed = parseContent(unescaped, ctx.niche(), ctx.prompt());
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
        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" hipóteses em formato JSON. ");
        sb.append("Use o seguinte nicho como contexto:\n");
        sb.append("Nome: ").append(niche.getName()).append("\n");
        if (niche.getDescription() != null) {
            sb.append("Descrição: ").append(niche.getDescription()).append("\n");
        }
        if (niche.getBaseSegmentation() != null) {
            sb.append("Segmentação base: ").append(niche.getBaseSegmentation()).append("\n");
        }
        if (niche.getInterests() != null) {
            sb.append("Interesses: ").append(niche.getInterests()).append("\n");
        }
        if (niche.getDemographicFilters() != null) {
            sb.append("Filtros demográficos: ").append(niche.getDemographicFilters()).append("\n");
        }
        if (niche.getExtraTips() != null) {
            sb.append("Dicas extras: ").append(niche.getExtraTips()).append("\n");
        }
        List<PromptAttribute> attrs = attributeRepository.findByEntity_Name("hypothesis");
        List<Long> descriptionIds = new ArrayList<>();
        Map<String, String> descriptions = attrs.stream()
                .map(attr -> {
                    var opt = descriptionRepository.findByAttribute_IdAndActiveTrue(attr.getId());
                    return opt.map(d -> {
                        descriptionIds.add(d.getId());
                        return Map.entry(attr.getName(), d.getDescription());
                    });
                })
                .flatMap(java.util.Optional::stream)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!descriptions.isEmpty()) {
            sb.append("Cada objeto deve conter as chaves: ")
                    .append(descriptions.keySet().stream().map(n -> "\\\"" + n + "\\\"")
                            .collect(java.util.stream.Collectors.joining(", ")))
                    .append(". ");
            descriptions.forEach((name, desc) ->
                    sb.append("Campo \\\"" + name + "\\\": " + desc + ". "));
        } else {
            sb.append("Cada objeto deve conter as chaves: \"title\", \"promise\", \"problem\", \"persona\", \"mechanism\", \"uniqueMechanism\", \"entrega\", \"successRule\", \"offerType\", \"price\". ");
        }
        sb.append("O campo \"offerType\" deve ser \"LEAD\" ou \"TRIPWIRE\". ");
        sb.append("O campo \"price\" deve ser um número. ");
        sb.append("Retorne apenas um array JSON com esses objetos, sem texto adicional.");
        return new PromptData(sb.toString(), descriptionIds);
    }

    private List<CreateHypothesisRequest> parseContent(String content, MarketNiche niche, PromptData data) throws Exception {
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
        for (CreateHypothesisRequest req : arr) {
            req.setMarketNicheId(niche.getId());
            req.setPrompt(data.prompt());
            req.setModel(model);
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

    private static String customId(MarketNiche niche) {
        return "niche-" + niche.getId();
    }

    private record PromptData(String prompt, List<Long> descriptionIds) {}

    public record HypothesisBatchRequest(MarketNiche niche, int quantity) {}

    private record RequestContext(MarketNiche niche, PromptData prompt, Map<String, Object> payload) {}

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
