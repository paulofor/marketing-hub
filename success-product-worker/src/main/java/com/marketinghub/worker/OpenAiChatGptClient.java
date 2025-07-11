package com.marketinghub.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * OpenAI client que suporta Function Calling + busca na web via Bing Search API.
 */
@Component
public class OpenAiChatGptClient implements ChatGptClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatGptClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String bingKey;

    public OpenAiChatGptClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:o3}") String model,
            @Value("${bing.api-key:}") String bingKey) {
        this.apiKey = apiKey;
        this.model = model;
        this.bingKey = bingKey;
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

        // ===== 1. Mensagens iniciais
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "Você é um especialista em marketing."));
        String prompt = "Preencha os campos explicitPain, promise, uniqueMechanism, " +
                "tripwire, riskReversal, socialProof, checkoutMonetization, funnel, " +
                "creativeVolume, storytelling em formato JSON.";
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
                String requestBody = MAPPER.writeValueAsString(Map.of(
                        "model", model,
                        "messages", messages,
                        "tools", tools,
                        "tool_choice", "auto"));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .timeout(Duration.ofMinutes(2))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                JsonNode choice = MAPPER.readTree(response.body())
                        .path("choices").get(0);
                String finishReason = choice.path("finish_reason").asText();

                // ===== 3a. Modelo quer usar o tool search_web
                if ("tool".equals(finishReason)) {
                    String query = choice.path("message").path("tool_call")
                            .path("arguments").path("query").asText();
                    List<SearchResult> results = searchWeb(query);
                    String toolContent = MAPPER.writeValueAsString(Map.of("results", results));

                    messages.add(Map.of(
                            "role", "tool",
                            "name", "search_web",
                            "content", toolContent));
                    continue; // volta ao início do loop
                }

                // ===== 3b. Resposta final do modelo
                String content = choice.path("message").path("content").asText();
                content = stripCodeFence(content);
                JsonNode data = MAPPER.readTree(content);

                product.setExplicitPain(asText(data, "explicitPain"));
                product.setPromise(asText(data, "promise"));
                product.setUniqueMechanism(asText(data, "uniqueMechanism"));
                product.setTripwire(asText(data, "tripwire"));
                product.setRiskReversal(asText(data, "riskReversal"));
                product.setSocialProof(asText(data, "socialProof"));
                product.setCheckoutMonetization(asText(data, "checkoutMonetization"));
                product.setFunnel(asText(data, "funnel"));
                product.setCreativeVolume(asText(data, "creativeVolume"));
                product.setStorytelling(asText(data, "storytelling"));
                product.setNovo(false);
                return product;
            }
        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            return product;
        }
    }

    /**
     * Executa busca na Web usando Bing Search API.
     */
    private List<SearchResult> searchWeb(String query) throws Exception {
        if (bingKey == null || bingKey.isBlank()) {
            return Collections.emptyList();
        }
        String endpoint = "https://api.bing.microsoft.com/v7.0/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Ocp-Apim-Subscription-Key", bingKey)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode webPages = MAPPER.readTree(resp.body()).path("webPages").path("value");

        List<SearchResult> list = new ArrayList<>();
        for (JsonNode item : webPages) {
            list.add(new SearchResult(
                    item.path("name").asText(),
                    item.path("url").asText(),
                    item.path("snippet").asText()));
        }
        return list.size() > 5 ? list.subList(0, 5) : list;
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
