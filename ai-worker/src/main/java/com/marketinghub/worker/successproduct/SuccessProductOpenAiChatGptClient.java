package com.marketinghub.worker.successproduct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI implementation that asks ChatGPT to extract a market niche and
 * hypothesis from a success product description.
 */
@Component
@Profile("!test & !dummy")
public class SuccessProductOpenAiChatGptClient implements ChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(SuccessProductOpenAiChatGptClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public SuccessProductOpenAiChatGptClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-3.5-turbo}") String model) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public NicheHypothesis extract(SuccessProduct product) {
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em marketing."),
                OpenAiRequestUtils.message("user", buildPrompt(product))
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Sending prompt to ChatGPT for product {}", product.getId());
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        log.info("ChatGPT raw response: {}", response);

        if (response == null) {
            log.warn("ChatGPT returned no choices for product {}", product.getId());
            return null;
        }
        if (response.hasError()) {
            log.error("OpenAI error while processing product {}: {}", product.getId(), response.errorMessage());
            return null;
        }
        String content = response.firstText();
        if (content == null || content.isBlank()) {
            log.warn("ChatGPT returned empty content for product {}", product.getId());
            return null;
        }
        log.info("ChatGPT content: {}", content);
        try {
            JsonNode node = objectMapper.readTree(content);
            return new NicheHypothesis(
                    asText(node, "nicheName"),
                    asText(node, "nicheDescription"),
                    asText(node, "hypothesisTitle"),
                    asText(node, "persona"),
                    asText(node, "problem"),
                    asText(node, "promise"),
                    asText(node, "uniqueMechanism"));
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            return null;
        }
    }

    private String buildPrompt(SuccessProduct product) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analise o seguinte produto de sucesso e retorne um JSON com as chaves ")
                .append("\"nicheName\", \"nicheDescription\", \"hypothesisTitle\", \"persona\", \"problem\", \"promise\", \"uniqueMechanism\".\n");
        if (product.getDescription() != null) {
            sb.append("Descrição: ").append(product.getDescription()).append("\n");
        }
        if (product.getName() != null) {
            sb.append("Nome: ").append(product.getName()).append("\n");
        }
        return sb.toString();
    }

    private static String asText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

}
