package com.marketinghub.worker.creative;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
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
 * Simple wrapper around the OpenAI chat completions API for creative generation.
 */
@Component
public class CreativeChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private static final Logger log = LoggerFactory.getLogger(CreativeChatGptClient.class);

    public CreativeChatGptClient(WebClient.Builder builder,
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

    public List<CreateCreativeRequest> generateCreatives(Experiment experiment, int quantity) {
        String prompt = buildPrompt(experiment, quantity);
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "Você é um especialista em marketing."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        log.info("Sending prompt to ChatGPT for experiment {}: {}", experiment.getId(), prompt);
        log.debug("ChatGPT payload: {}", payload);

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        log.info("ChatGPT raw response: {}", response);

        if (response == null || response.choices().isEmpty()) {
            log.warn("ChatGPT returned no choices for experiment {}", experiment.getId());
            return List.of();
        }
        String content = response.choices().get(0).message().content();
        log.info("ChatGPT content: {}", content);
        try {
            List<CreateCreativeRequest> parsed = parseContent(content);
            log.info("Parsed creatives: {}", parsed);
            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            try {
                String unescaped = content.replace("\\\"", "\"");
                List<CreateCreativeRequest> parsed = parseContent(unescaped);
                log.info("Parsed creatives after unescaping: {}", parsed);
                return parsed;
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT response: {}", content, ex);
                throw new RuntimeException("Failed to parse ChatGPT response", ex);
            }
        }
    }

    private String buildPrompt(Experiment experiment, int quantity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" criativos em formato JSON. ");
        Hypothesis h = experiment.getHypothesisRef();
        if (h != null) {
            sb.append("Use a seguinte hipótese como contexto:\n");
            sb.append("Título: ").append(h.getTitle()).append("\n");
            if (h.getPromise() != null) sb.append("Promessa: ").append(h.getPromise()).append("\n");
            if (h.getProblem() != null) sb.append("Problema: ").append(h.getProblem()).append("\n");
            if (h.getPersona() != null) sb.append("Persona: ").append(h.getPersona()).append("\n");
            if (h.getMechanism() != null) sb.append("Mecanismo: ").append(h.getMechanism()).append("\n");
            if (h.getUniqueMechanism() != null) sb.append("Mecanismo único: ").append(h.getUniqueMechanism()).append("\n");
            if (h.getEntrega() != null) sb.append("Entrega: ").append(h.getEntrega()).append("\n");
            if (h.getSuccessRule() != null) sb.append("Regra de sucesso: ").append(h.getSuccessRule()).append("\n");
            if (h.getOfferType() != null) sb.append("Tipo de oferta: ").append(h.getOfferType()).append("\n");
            if (h.getPrice() != null) sb.append("Preço: ").append(h.getPrice()).append("\n");
        }
        sb.append("Cada objeto deve conter as chaves: \"headline\" (máximo 255 caracteres), \"primaryText\" (máximo 255 caracteres). ");
        sb.append("Retorne apenas um array JSON com esses objetos, sem texto adicional.");
        return sb.toString();
    }

    private List<CreateCreativeRequest> parseContent(String content) throws Exception {
        CreateCreativeRequest[] arr = objectMapper.readValue(content, CreateCreativeRequest[].class);
        for (CreateCreativeRequest req : arr) {
            if (req.getStatus() == null) {
                req.setStatus(CreativeStatus.DRAFT);
            }
        }
        return Arrays.asList(arr);
    }

    private record ChatCompletionResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}
