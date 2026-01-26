package com.marketinghub.worker.audience;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.audience.AudienceSource;
import com.marketinghub.audience.TargetingSeedStatus;
import com.marketinghub.audience.TargetingSeedType;
import com.marketinghub.audience.dto.AudienceTargetingSeedRequest;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wrapper do ChatGPT para geração de públicos de Meta Ads.
 */
@Component
public class AudienceChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(AudienceChatGptClient.class);
    private static final String DOMAIN = "AUDIENCE_SEGMENT";
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final String COMPLETION_WINDOW = "24h";
    private static final Duration BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration BATCH_TIMEOUT = Duration.ofMinutes(2);
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    public AudienceChatGptClient(WebClient.Builder builder,
                                 ObjectMapper objectMapper,
                                 @Value("${openai.api-key:}") String apiKey,
                                 @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                 @Value("${openai.model:gpt-3.5-turbo}") String model,
                                 AiGenerationRecorder generationRecorder) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.defaultModel = model;
        this.generationRecorder = generationRecorder;
    }

    public List<CreateAudienceRequest> generateAudiences(MarketNiche niche,
                                                         List<Hypothesis> hypotheses,
                                                         int quantity) {
        if (niche == null) {
            return List.of();
        }
        Map<Long, List<CreateAudienceRequest>> grouped = generateAudiencesBatch(
                List.of(new AudienceBatchRequest(niche, hypotheses, quantity, null)));
        return grouped.getOrDefault(niche.getId(), List.of());
    }

    public Map<Long, List<CreateAudienceRequest>> generateAudiencesBatch(List<AudienceBatchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }
        Map<String, RequestContext> contexts = new LinkedHashMap<>();
        for (AudienceBatchRequest request : requests) {
            if (request == null || request.niche() == null) {
                continue;
            }
            int quantity = Math.max(0, request.quantity());
            if (quantity == 0) {
                continue;
            }
            MarketNiche niche = request.niche();
            List<Hypothesis> hypotheses = request.hypotheses() != null ? request.hypotheses() : List.of();
            PromptData promptData = buildPrompt(niche, hypotheses, quantity);
            List<Map<String, Object>> input = List.of(
                    OpenAiRequestUtils.message("system", "Você é um especialista em marketing e segmentação para anúncios da Meta."),
                    OpenAiRequestUtils.message("user", promptData.prompt())
            );
            String modelToUse = resolveModel(request.model());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", modelToUse);
            payload.put("input", input);
            OpenAiRequestUtils.maybeAddReasoning(payload, modelToUse);
            String customId = customId(niche);
            contexts.put(customId, new RequestContext(niche, hypotheses, promptData, payload, modelToUse, quantity));
        }
        if (contexts.isEmpty()) {
            return Map.of();
        }
        Map<String, OpenAiResponse> responses = executeBatchRequests(contexts);
        Map<Long, List<CreateAudienceRequest>> result = new LinkedHashMap<>();
        for (Map.Entry<String, RequestContext> entry : contexts.entrySet()) {
            RequestContext ctx = entry.getValue();
            OpenAiResponse response = responses.get(entry.getKey());
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
            if (content == null || content.isBlank()) {
                log.warn("ChatGPT returned empty content for niche {}", ctx.niche().getId());
                continue;
            }
            try {
                List<CreateAudienceRequest> parsed = parseContent(content, ctx.niche(), ctx.prompt(), ctx.quantity(), ctx.model());
                result.put(ctx.niche().getId(), parsed);
            } catch (Exception e) {
                log.error("Failed to parse ChatGPT response for niche {}: {}", ctx.niche().getId(), content, e);
                try {
                    String unescaped = content.replace("\\\"", "\"");
                    List<CreateAudienceRequest> parsed = parseContent(unescaped, ctx.niche(), ctx.prompt(), ctx.quantity(), ctx.model());
                    result.put(ctx.niche().getId(), parsed);
                } catch (Exception ex) {
                    log.error("Failed to parse unescaped ChatGPT response for niche {}: {}", ctx.niche().getId(), content, ex);
                    throw new RuntimeException("Failed to parse ChatGPT response", ex);
                }
            }
        }
        return result;
    }

    private PromptData buildPrompt(MarketNiche niche, List<Hypothesis> hypotheses, int quantity) {
        Set<UUID> hypothesisIds = hypotheses.stream()
                .map(Hypothesis::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" públicos para campanhas de Meta Ads em formato JSON. ");
        sb.append("Cada objeto deve conter as chaves \"name\", \"description\", \"hypothesisId\" e \"segments\". ");
        sb.append("Use null em \"hypothesisId\" quando o público for geral do nicho.\n");
        sb.append("O campo \"segments\" deve incluir arrays de interesses, comportamentos, cargos, dados demográficos e localizações reais do Meta Ads. ");
        sb.append("Cada item pode ser texto ou um objeto com \"value\", \"confidence\" (0 a 1) e notas curtas. \n");
        sb.append("Combine comportamentos, interesses, cargos e regiões para refletir o Meta Audience Targeting. Limite cada lista a no máximo 5 itens.\n");
        sb.append("Nicho:\n");
        appendField(sb, "Nome", niche.getName());
        appendField(sb, "Descrição", niche.getDescription());
        appendField(sb, "Segmentação base", niche.getBaseSegmentation());
        appendField(sb, "Interesses", niche.getInterests());
        appendField(sb, "Filtros demográficos", niche.getDemographicFilters());
        appendField(sb, "Dicas extras", niche.getExtraTips());

        if (!hypotheses.isEmpty()) {
            sb.append("Hipóteses disponíveis com seus identificadores:\n");
            for (Hypothesis h : hypotheses) {
                sb.append("- ID: ").append(h.getId()).append(" | Título: ").append(truncate(h.getTitle(), 140)).append("\n");
                appendIndentedField(sb, "Promessa", h.getPromise());
                appendIndentedField(sb, "Persona", h.getPersona());
                appendIndentedField(sb, "Mecanismo", h.getMechanism());
                appendIndentedField(sb, "Mecanismo único", h.getUniqueMechanism());
            }
            sb.append("Use o campo hypothesisId com um dos IDs listados quando o público for específico de uma hipótese.\n");
        } else {
            sb.append("Não há hipóteses cadastradas. Gere públicos gerais do nicho e defina hypothesisId como null.\n");
        }
        sb.append("Retorne apenas um array JSON, sem comentários extras.");
        return new PromptData(sb.toString(), hypothesisIds);
    }

    private List<CreateAudienceRequest> parseContent(String content,
                                                     MarketNiche niche,
                                                     PromptData data,
                                                     int quantity,
                                                     String model) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        JsonNode array = root;
        if (root.isObject()) {
            if (root.has("audiences")) {
                array = root.get("audiences");
            } else if (root.has("items")) {
                array = root.get("items");
            }
        }
        if (!array.isArray()) {
            throw new IllegalArgumentException("Expected JSON array with audiences");
        }
        List<CreateAudienceRequest> result = new ArrayList<>();
        for (JsonNode node : array) {
            CreateAudienceRequest req = new CreateAudienceRequest();
            req.setName(asText(node, "name"));
            req.setDescription(asText(node, "description"));
            String hypothesisIdText = asText(node, "hypothesisId");
            if (hypothesisIdText != null && !hypothesisIdText.isBlank() && !"null".equalsIgnoreCase(hypothesisIdText)) {
                try {
                    UUID hypothesisId = UUID.fromString(hypothesisIdText.trim());
                    if (data.hypothesisIds().contains(hypothesisId)) {
                        req.setHypothesisId(hypothesisId);
                    } else {
                        log.warn("Ignoring hypothesisId {} not linked to niche {}", hypothesisId, niche.getId());
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid hypothesisId '{}' for niche {}", hypothesisIdText, niche.getId());
                }
            }
            req.setMarketNicheId(niche.getId());
            req.setPrompt(data.prompt());
            req.setModel(model);
            req.setSource(AudienceSource.AI);
            JsonNode segments = node.get("segments");
            if (segments != null && segments.isObject()) {
                List<AudienceTargetingSeedRequest> seeds = new ArrayList<>();
                seeds.addAll(extractSeeds(segments, "interests", TargetingSeedType.INTEREST));
                seeds.addAll(extractSeeds(segments, "behaviors", TargetingSeedType.BEHAVIOR));
                seeds.addAll(extractSeeds(segments, "jobTitles", TargetingSeedType.DEMOGRAPHIC));
                seeds.addAll(extractSeeds(segments, "demographics", TargetingSeedType.DEMOGRAPHIC));
                seeds.addAll(extractSeeds(segments, "locations", TargetingSeedType.LOCATION));
                seeds.addAll(extractSeeds(segments, "customAudiences", TargetingSeedType.CUSTOM_AUDIENCE));
                seeds.addAll(extractSeeds(segments, "lookalikes", TargetingSeedType.LOOKALIKE));
                if (!seeds.isEmpty()) {
                    req.setSeeds(seeds);
                }
            }
            result.add(req);
            if (result.size() >= quantity) {
                break;
            }
        }
        return result;
    }

    private List<AudienceTargetingSeedRequest> extractSeeds(JsonNode parent,
                                                            String field,
                                                            TargetingSeedType type) {
        JsonNode array = parent.get(field);
        if (array == null || array.isNull() || !array.isArray()) {
            return List.of();
        }
        List<AudienceTargetingSeedRequest> seeds = new ArrayList<>();
        for (JsonNode item : array) {
            String value = null;
            if (item.isTextual()) {
                value = item.asText();
            } else if (item.isObject()) {
                value = asText(item, "value");
                if (value == null) {
                    value = asText(item, "name");
                }
            }
            if (value == null || value.isBlank()) {
                continue;
            }
            AudienceTargetingSeedRequest seed = new AudienceTargetingSeedRequest();
            seed.setType(type);
            seed.setValue(value.trim());
            if (item.isObject()) {
                seed.setMetaId(asText(item, "metaId"));
                seed.setKey(asText(item, "key"));
                seed.setConfidence(parseConfidence(item.get("confidence")));
            }
            seed.setStatus(TargetingSeedStatus.DRAFT);
            seeds.add(seed);
        }
        return seeds;
    }

    private BigDecimal parseConfidence(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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
                sb.append(objectMapper.writeValueAsString(line)).append('\n');
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize batch line for " + entry.getKey(), e);
            }
        }
        byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return "audience-segments.jsonl";
            }
        };
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);
        log.info("Uploading {} audience prompts to OpenAI batch file", contexts.size());
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
        return "audience-" + niche.getId();
    }

    private static String asText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(truncate(value, 500)).append("\n");
        }
    }

    private static void appendIndentedField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("  ").append(label).append(": ").append(truncate(value, 300)).append("\n");
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private record PromptData(String prompt, Set<UUID> hypothesisIds) {}

    public record AudienceBatchRequest(MarketNiche niche,
                                       List<Hypothesis> hypotheses,
                                       int quantity,
                                       String model) {}

    private record RequestContext(MarketNiche niche,
                                  List<Hypothesis> hypotheses,
                                  PromptData prompt,
                                  Map<String, Object> payload,
                                  String model,
                                  int quantity) {}

    private record BatchOutput(String id,
                               String customId,
                               BatchResponse response,
                               BatchError error) {}

    private record BatchResponse(Integer statusCode, Map<String, Object> body) {
        private boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private record BatchError(String code, String message) {}

    private record OpenAiBatch(String id, String status, String outputFileId) {}

    private record OpenAiFile(String id) {}
}
