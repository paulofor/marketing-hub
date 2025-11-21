package com.marketinghub.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiApiKeyProvider;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI client que suporta Function Calling + busca na web via Google Search API.
 */
@Component
public class OpenAiChatGptClient implements ChatGptClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatGptClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String googleKey;
    private final String searchId;
    private final AiGenerationRecorder generationRecorder;
    private final boolean enabled;
    private static final String DOMAIN = "SUCCESS_PRODUCT_ENRICHMENT";

    public OpenAiChatGptClient(
            OpenAiApiKeyProvider apiKeyProvider,
            @Value("${openai.model:o3}") String model,
            @Value("${google.api-key:}") String googleKey,
            @Value("${google.search-id:}") String searchId,
            AiGenerationRecorder generationRecorder) {
        this.apiKey = apiKeyProvider.getApiKey();
        this.model = model;
        this.googleKey = googleKey;
        this.searchId = searchId;
        this.enabled = apiKeyProvider.isConfigured();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.generationRecorder = generationRecorder;
    }

    @Override
    public SuccessProduct enrich(SuccessProduct product) {
        if (!enabled) {
            log.warn("OpenAI API key not configured, returning product unchanged");
            return product;
        }

        log.info("Enriching product {} with OpenAI", product.getId());

        // ===== 1. Mensagens iniciais
        String prompt = "Preencha os campos name, explicitPain, promise, uniqueMechanism, " +
                "tripwire, riskReversal, socialProof, checkoutMonetization, salesFunnel, audienceType, " +
                "creativeVolume, storytelling, salesPageUrl, instagramUrl, facebookUrl, " +
                "youtubeUrl em formato JSON. Se houver um link de p\u00e1gina de vendas na\n" +
                "descri\u00e7\u00e3o, visite a p\u00e1gina para coletar esses detalhes de copy e\n" +
                "marketing, incluindo links de redes sociais.";

        Map<String, Object> searchTool = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "search_web",
                        "description", "Busca na Internet e devolve até 5 resultados relevantes.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of("query", Map.of("type", "string")),
                                "required", List.of("query"))));
        List<Object> tools = List.of(searchTool);

        try {
            List<Map<String, Object>> input = new ArrayList<>();
            input.add(OpenAiRequestUtils.message("system", "Você é um especialista em marketing."));
            input.add(OpenAiRequestUtils.message("user", prompt + "\n" + product.getDescription()));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", input);
            payload.put("tools", tools);
            payload.put("tool_choice", "auto");
            OpenAiRequestUtils.maybeAddReasoning(payload, model);

            OpenAiResponse response = executeResponseRequest(payload);

            while (true) {
                if (response == null) {
                    log.error("OpenAI returned null response for product {}", product.getId());
                    return product;
                }
                if (response.hasError()) {
                    log.error("OpenAI error: {}", response.errorMessage());
                    return product;
                }

                List<OpenAiResponse.OpenAiToolCall> toolCalls = response.firstToolCalls();
                if (!toolCalls.isEmpty()) {
                    OpenAiResponse.OpenAiToolCall toolCall = toolCalls.get(0);
                    String functionName = toolCall.function() != null ? toolCall.function().name() : null;
                    if (!"search_web".equals(functionName)) {
                        log.warn("Ignoring unsupported tool {} for product {}", functionName, product.getId());
                        return product;
                    }
                    String query = extractSearchQuery(toolCall);
                    if (query == null || query.isBlank()) {
                        log.warn("Search tool call without query for product {}", product.getId());
                        return product;
                    }
                    log.info("Searching web for '{}'", query);
                    List<SearchResult> results = searchWeb(query);
                    log.info("Search returned {} results", results.size());
                    String toolContent = MAPPER.writeValueAsString(Map.of("results", results));

                    Map<String, Object> followUp = new LinkedHashMap<>();
                    followUp.put("model", model);
                    followUp.put("previous_response_id", response.id());
                    followUp.put("input", List.of(OpenAiRequestUtils.toolOutput(toolCall.effectiveCallId(), toolContent)));
                    followUp.put("tools", tools);
                    followUp.put("tool_choice", "auto");
                    OpenAiRequestUtils.maybeAddReasoning(followUp, model);

                    response = executeResponseRequest(followUp);
                    continue;
                }

                String content = response.firstText();
                if (content == null || content.isBlank()) {
                    log.error("OpenAI returned empty content for product {}", product.getId());
                    return product;
                }

                generationRecorder.record(DOMAIN,
                        product != null ? String.valueOf(product.getId()) : null,
                        prompt,
                        content,
                        model,
                        response.usage());

                content = stripCodeFence(content);
                JsonNode data = MAPPER.readTree(content);

                product.setName(asText(data, "name"));
                product.setExplicitPain(asText(data, "explicitPain"));
                product.setPromise(asText(data, "promise"));
                product.setUniqueMechanism(asText(data, "uniqueMechanism"));
                product.setTripwire(asText(data, "tripwire"));
                product.setRiskReversal(asText(data, "riskReversal"));
                product.setSocialProof(asText(data, "socialProof"));
                product.setCheckoutMonetization(asText(data, "checkoutMonetization"));
                product.setSalesFunnel(asText(data, "salesFunnel"));
                product.setAudienceType(asText(data, "audienceType"));
                product.setCreativeVolume(asText(data, "creativeVolume"));
                product.setStorytelling(asText(data, "storytelling"));
                product.setSalesPageUrl(asText(data, "salesPageUrl"));
                product.setInstagramUrl(asText(data, "instagramUrl"));
                product.setFacebookUrl(asText(data, "facebookUrl"));
                product.setYoutubeUrl(asText(data, "youtubeUrl"));
                product.setNovo(false);
                log.info("OpenAI enrichment completed for product {}", product.getId());
                return product;
            }
        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            return product;
        }
    }

    /**
     * Executa busca na Web usando Google Search API.
     */
    private List<SearchResult> searchWeb(String query) throws Exception {
        if (googleKey == null || googleKey.isBlank() || searchId == null || searchId.isBlank()) {
            return Collections.emptyList();
        }
        String endpoint = "https://www.googleapis.com/customsearch/v1?key=" + googleKey +
                "&cx=" + URLEncoder.encode(searchId, StandardCharsets.UTF_8) +
                "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode items = MAPPER.readTree(resp.body()).path("items");

        List<SearchResult> list = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                list.add(new SearchResult(
                        item.path("title").asText(),
                        item.path("link").asText(),
                        item.path("snippet").asText()));
                if (list.size() == 5) break;
            }
        }
        return list;
    }

    private OpenAiResponse executeResponseRequest(Map<String, Object> payload) throws Exception {
        String requestBody = MAPPER.writeValueAsString(payload);
        log.info("Sending OpenAI Responses request: {}", requestBody);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/responses"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json");
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            builder.header("OpenAI-Beta", "reasoning=1");
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("OpenAI response body: {}", response.body());
        return MAPPER.readValue(response.body(), OpenAiResponse.class);
    }

    private String extractSearchQuery(OpenAiResponse.OpenAiToolCall toolCall) {
        if (toolCall == null || toolCall.function() == null) {
            return null;
        }
        String args = toolCall.function().arguments();
        if (args == null || args.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(args);
            JsonNode queryNode = node.path("query");
            if (queryNode.isTextual()) {
                return queryNode.asText();
            }
            return queryNode.isNull() ? null : queryNode.toString();
        } catch (Exception e) {
            log.warn("Failed to parse tool arguments: {}", args, e);
            return null;
        }
    }

    private static String stripCodeFence(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("```") && t.contains("```")) {
            int start = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (start >= 0 && end > start) return t.substring(start + 1, end).trim();
        }
        return t;
    }

    private static String asText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    /** Resultado simplificado de busca. */
    public static class SearchResult {
        public final String title;
        public final String url;
        public final String snippet;
        public SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
