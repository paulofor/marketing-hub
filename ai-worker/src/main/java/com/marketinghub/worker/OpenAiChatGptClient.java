package com.marketinghub.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.successproduct.SuccessProduct;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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

    public OpenAiChatGptClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:o3}") String model,
            @Value("${google.api-key:}") String googleKey,
            @Value("${google.search-id:}") String searchId) {
        this.apiKey = apiKey;
        this.model = model;
        this.googleKey = googleKey;
        this.searchId = searchId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public SuccessProduct enrich(SuccessProduct product) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured, returning product unchanged");
            return product;
        }

        log.info("Enriching product {} with OpenAI", product.getId());

        // ===== 1. Mensagens iniciais
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "Você é um especialista em marketing."));
        String prompt = "Preencha os campos name, explicitPain, promise, uniqueMechanism, " +
                "tripwire, riskReversal, socialProof, checkoutMonetization, salesFunnel, audienceType, " +
                "creativeVolume, storytelling, salesPageUrl, instagramUrl, facebookUrl, " +
                "youtubeUrl em formato JSON. Se houver um link de p\u00e1gina de vendas na\n" +
                "descri\u00e7\u00e3o, visite a p\u00e1gina para coletar esses detalhes de copy e\n" +
                "marketing, incluindo links de redes sociais.";
        messages.add(Map.of("role", "user", "content", prompt + "\n" + product.getDescription()));

        // ===== 2. Definição do tool search_web
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
            // ===== 3. Loop principal (tool calling)
            while (true) {
                boolean structuredOutputs = false; // TODO ativar para usar Structured Outputs e evitar cercas
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("model", model);
                body.put("messages", messages);
                body.put("tools", tools);
                body.put("tool_choice", "auto");
                if (structuredOutputs) {
                    body.put("response_format", buildResponseFormat());
                }
                String requestBody = MAPPER.writeValueAsString(body);

                log.info("Sending ChatGPT request with {} messages", messages.size());
                log.info("ChatGPT request body: {}", requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .timeout(Duration.ofMinutes(2))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                log.info("ChatGPT response body: {}", response.body());

                JsonNode root = MAPPER.readTree(response.body());
                if (root.has("error")) {
                    log.error("OpenAI error: {}", root.path("error").path("message").asText());
                    return product;
                }

                JsonNode choice = root.path("choices").get(0);
                if (choice == null || choice.isNull()) {
                    log.error("OpenAI response missing choices");
                    return product;
                }

                JsonNode messageNode = choice.path("message");
                // add the assistant message so the next request keeps the conversation context
                @SuppressWarnings("unchecked")
                Map<String, Object> assistantMsg = MAPPER.convertValue(messageNode, Map.class);
                messages.add(assistantMsg);

                String finishReason = choice.path("finish_reason").asText();

                log.info("OpenAI finish reason: {}", finishReason);

                // ===== 3a. Modelo quer usar o tool search_web
                if ("tool".equals(finishReason) || "tool_calls".equals(finishReason)) {
                    JsonNode toolCall = choice.path("message").path("tool_call");
                    if (toolCall == null || toolCall.isMissingNode() || toolCall.isNull()) {
                        JsonNode array = choice.path("message").path("tool_calls");
                        if (array.isArray() && array.size() > 0) {
                            toolCall = array.get(0);
                        }
                    }

                    String callId = toolCall.path("id").asText();
                    String query = toolCall.path("function").path("arguments").path("query").asText();
                    log.info("Searching web for '{}'", query);
                    List<SearchResult> results = searchWeb(query);
                    log.info("Search returned {} results", results.size());
                    String toolContent = MAPPER.writeValueAsString(Map.of("results", results));

                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", callId,
                            "name", "search_web",
                            "content", toolContent));
                    continue; // volta ao início do loop
                }

                // ===== 3b. Resposta final do modelo
                String content = choice.path("message").path("content").asText();
                JsonNode data;
                try {
                    data = JsonUtils.parsePossiblyDoubleEncoded(content, new TypeReference<JsonNode>() {});
                } catch (JsonProcessingException e) {
                    String preview = content == null ? "null" : content.substring(0, Math.min(200, content.length()));
                    log.error("Failed to parse ChatGPT response: {}", preview, e);
                    return product;
                }

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

    // TODO usar Structured Outputs para forçar o modelo a seguir o schema e evitar Markdown
    private Map<String, Object> buildResponseFormat() {
        Map<String, Object> properties = Map.of(
                "title", Map.of("type", "string"),
                "promise", Map.of("type", "string"),
                "problem", Map.of("type", "string"),
                "persona", Map.of("type", "string"),
                "successRule", Map.of("type", "string"),
                "offerType", Map.of("type", "string"),
                "kpiTargetCpl", Map.of("type", "number"));
        Map<String, Object> itemSchema = Map.of(
                "type", "object",
                "properties", properties);
        Map<String, Object> jsonSchema = Map.of(
                "type", "array",
                "items", itemSchema,
                "strict", true);
        return Map.of(
                "type", "json_schema",
                "json_schema", jsonSchema);
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
