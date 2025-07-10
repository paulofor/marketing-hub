package com.marketinghub.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ChatGptClient implementation that calls the OpenAI API.
 */
@Component
public class OpenAiChatGptClient implements ChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatGptClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final String apiKey;
    private final String model;

    public OpenAiChatGptClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:o3}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public SuccessProduct enrich(SuccessProduct product) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured, returning product unchanged");
            return product;
        }
        try {
            String prompt = "Preencha os campos explicitPain, promise, uniqueMechanism, "
                    + "tripwire, riskReversal, socialProof, checkoutMonetization, funnel, "
                    + "creativeVolume, storytelling em formato JSON.";
            String requestBody = MAPPER.writeValueAsString(
                    Map.of(
                            "model",
                            model,
                            "messages",
                            List.of(
                                    Map.of("role", "system", "content", "Você é um especialista em marketing."),
                                    Map.of(
                                            "role",
                                            "user",
                                            "content",
                                            prompt + "\n" + product.getDescription())),
                            "temperature",
                            0));

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                            .timeout(Duration.ofMinutes(2))
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            JsonNode root = MAPPER.readTree(body);
            String content = root.path("choices").get(0).path("message").path("content").asText();
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
        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            return product;
        }
    }

    private static String asText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
