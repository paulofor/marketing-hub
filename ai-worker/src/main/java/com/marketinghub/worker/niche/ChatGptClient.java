package com.marketinghub.worker.niche;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.niche.MarketNiche;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Simple wrapper around the OpenAI chat completions API.
 */
@Component
public class ChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private static final Logger log = LoggerFactory.getLogger(ChatGptClient.class);

    public ChatGptClient(WebClient.Builder builder,
                         ObjectMapper objectMapper,
                         @Value("${openai.api-key:}") String apiKey,
                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                         @Value("${openai.model:gpt-3.5-turbo}") String model) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public List<CreateHypothesisRequest> generateHypotheses(MarketNiche niche, int quantity) {
        String prompt = buildPrompt(niche, quantity);
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "Você é um especialista em marketing."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        log.info("Sending prompt to ChatGPT for niche {}: {}", niche.getId(), prompt);
        log.debug("ChatGPT payload: {}", payload);

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        log.debug("ChatGPT raw response: {}", response);

        if (response == null || response.choices().isEmpty()) {
            log.warn("ChatGPT returned no choices for niche {}", niche.getId());
            return List.of();
        }
        String content = response.choices().get(0).message().content();
        log.debug("ChatGPT content: {}", content);
        try {
            return parseContent(content, niche, prompt);
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            try {
                String unescaped = objectMapper.readValue("\"" + content + "\"", String.class);
                return parseContent(unescaped, niche, prompt);
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT response: {}", content, ex);
                throw new RuntimeException("Failed to parse ChatGPT response", ex);
            }
        }
    }

    private String buildPrompt(MarketNiche niche, int quantity) {
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
        sb.append("Retorne apenas o JSON com uma lista de objetos compatíveis com CreateHypothesisRequest.");
        return sb.toString();
    }

    private record ChatCompletionResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}

    private List<CreateHypothesisRequest> parseContent(String content, MarketNiche niche, String prompt) throws Exception {
        CreateHypothesisRequest[] arr = objectMapper.readValue(content, CreateHypothesisRequest[].class);
        for (CreateHypothesisRequest req : arr) {
            req.setMarketNicheId(niche.getId());
            req.setPrompt(prompt);
            req.setModel(model);
        }
        return Arrays.asList(arr);
    }
}
