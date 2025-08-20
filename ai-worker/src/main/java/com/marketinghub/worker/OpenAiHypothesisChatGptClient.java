package com.marketinghub.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.repository.AngleRepository;
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

@Component
public class OpenAiHypothesisChatGptClient implements HypothesisChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiHypothesisChatGptClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final AngleRepository angleRepository;

    public OpenAiHypothesisChatGptClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:o3}") String model,
            AngleRepository angleRepository) {
        this.apiKey = apiKey;
        this.model = model;
        this.angleRepository = angleRepository;
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
        try {
            String prompt = buildPrompt(niche, quantity);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Você é um especialista em marketing."),
                            Map.of("role", "user", "content", prompt)));
            String requestBody = MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(2))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String content = response.body();
            log.debug("ChatGPT response: {}", content);
            JsonNode root = MAPPER.readTree(content);
            JsonNode choice = root.path("choices").get(0);
            if (choice == null || choice.isNull()) {
                return Collections.emptyList();
            }
            String message = choice.path("message").path("content").asText();
            message = stripCodeFence(message);
            JsonNode array = MAPPER.readTree(message);
            List<Hypothesis> result = new ArrayList<>();
            for (JsonNode node : array) {
                String angleName = asText(node, "premiseAngle");
                Angle angle = angleRepository.save(Angle.builder().name(angleName).build());
                Hypothesis h = Hypothesis.builder()
                        .title(asText(node, "title"))
                        .promise(asText(node, "promise"))
                        .problem(asText(node, "problem"))
                        .persona(asText(node, "persona"))
                        .mechanism(asText(node, "mechanism"))
                        .uniqueMechanism(asText(node, "uniqueMechanism"))
                        .premiseAngle(angle)
                        .offerType(parseOfferType(asText(node, "offerType")))
                        .kpiTargetCpl(new BigDecimal(asText(node, "kpiTargetCpl", "0")))
                        .model(model)
                        .prompt(prompt)
                        .build();
                result.add(h);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            return Collections.emptyList();
        }
    }

    private static OfferType parseOfferType(String value) {
        try {
            return value != null ? OfferType.valueOf(value) : OfferType.LEAD_MAGNET;
        } catch (Exception e) {
            return OfferType.LEAD_MAGNET;
        }
    }

    private static String buildPrompt(MarketNiche niche, int quantity) {
        return "Gere " + quantity + " hipóteses em formato JSON para o nicho abaixo. Cada item deve conter os campos" +
                " title, promise, problem, persona, mechanism, uniqueMechanism, premiseAngle, offerType, kpiTargetCpl." +
                " Nicho: " + safe(niche.getName()) +
                "\nDescrição: " + safe(niche.getDescription()) +
                "\nSegmentação Base: " + safe(niche.getBaseSegmentation()) +
                "\nInteresses: " + safe(niche.getInterests()) +
                "\nDicas Extras: " + safe(niche.getExtraTips());
    }

    private static String safe(String text) {
        return text == null ? "" : text;
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
        return asText(node, field, null);
    }

    private static String asText(JsonNode node, String field, String def) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : def;
    }
}

