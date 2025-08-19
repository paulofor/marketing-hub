package com.marketinghub.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.niche.MarketNiche;
import java.math.BigDecimal;
import java.net.URI;
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
 * ChatGPT client para gerar hipóteses a partir de um nicho de mercado.
 */
@Component
public class OpenAiHypothesisChatGptClient implements HypothesisChatGptClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiHypothesisChatGptClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public OpenAiHypothesisChatGptClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public List<Hypothesis> generate(MarketNiche niche, int quantity) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured, returning empty list");
            return Collections.emptyList();
        }

        String prompt = buildPrompt(niche, quantity);
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", "Você é um especialista em marketing."),
                Map.of("role", "user", "content", prompt));

        try {
            String requestBody = MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofMinutes(2))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String content = MAPPER.readTree(response.body())
                    .path("choices").get(0).path("message").path("content").asText();
            content = stripCodeFence(content);
            JsonNode array = MAPPER.readTree(content);
            List<Hypothesis> result = new ArrayList<>();
            for (JsonNode node : array) {
                Hypothesis h = Hypothesis.builder()
                        .title(asText(node, "title"))
                        .promise(asText(node, "promise"))
                        .problem(asText(node, "problem"))
                        .persona(asText(node, "persona"))
                        .mechanism(asText(node, "mechanism"))
                        .uniqueMechanism(asText(node, "uniqueMechanism"))
                        .successRule(asText(node, "successRule"))
                        .offerType(parseOfferType(asText(node, "offerType")))
                        .price(parseBigDecimal(node, "price"))
                        .kpiTargetCpl(parseBigDecimal(node, "kpiTargetCpl"))
                        .build();
                result.add(h);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            return Collections.emptyList();
        }
    }

    private String buildPrompt(MarketNiche niche, int quantity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" hipóteses de marketing em formato JSON para o nicho abaixo.\n");
        sb.append("Nicho: ").append(niche.getName()).append("\n");
        if (niche.getDescription() != null) sb.append("Descrição: ").append(niche.getDescription()).append("\n");
        if (niche.getDemandVolume() != null) sb.append("Volume de demanda: ").append(niche.getDemandVolume()).append("\n");
        if (niche.getPromises() != null) sb.append("Promessas: ").append(niche.getPromises()).append("\n");
        if (niche.getOffers() != null) sb.append("Ofertas: ").append(niche.getOffers()).append("\n");
        if (niche.getBaseSegmentation() != null) sb.append("Segmentação base: ").append(niche.getBaseSegmentation()).append("\n");
        if (niche.getInterests() != null) sb.append("Interesses: ").append(niche.getInterests()).append("\n");
        if (niche.getDemographicFilters() != null) sb.append("Filtros demográficos: ").append(niche.getDemographicFilters()).append("\n");
        if (niche.getExtraTips() != null) sb.append("Dicas extras: ").append(niche.getExtraTips()).append("\n");
        sb.append("Responda com um array JSON onde cada item possui os campos title, promise, problem, persona, mechanism, uniqueMechanism, successRule, offerType (LEAD ou TRIPWIRE), price e kpiTargetCpl.");
        return sb.toString();
    }

    private static String asText(JsonNode node, String field) {
        return node.path(field).asText();
    }

    private static BigDecimal parseBigDecimal(JsonNode node, String field) {
        String text = node.path(field).asText("0");
        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static OfferType parseOfferType(String value) {
        try {
            return OfferType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return OfferType.LEAD;
        }
    }

    private static String stripCodeFence(String content) {
        return content.replaceAll("^```(json)?|```$", "").trim();
    }
}
