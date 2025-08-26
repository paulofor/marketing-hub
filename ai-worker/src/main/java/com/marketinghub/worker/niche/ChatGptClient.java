package com.marketinghub.worker.niche;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
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
    private final PromptAttributeRepository attributeRepository;
    private final PromptAttributeDescriptionRepository descriptionRepository;
    private static final Logger log = LoggerFactory.getLogger(ChatGptClient.class);

    public ChatGptClient(WebClient.Builder builder,
                         ObjectMapper objectMapper,
                         @Value("${openai.api-key:}") String apiKey,
                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                         @Value("${openai.model:gpt-3.5-turbo}") String model,
                         PromptAttributeRepository attributeRepository,
                         PromptAttributeDescriptionRepository descriptionRepository) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.attributeRepository = attributeRepository;
        this.descriptionRepository = descriptionRepository;
    }

    public List<CreateHypothesisRequest> generateHypotheses(MarketNiche niche, int quantity) {
        PromptData promptData = buildPrompt(niche, quantity);
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "Você é um especialista em marketing."),
                        Map.of("role", "user", "content", promptData.prompt())
                )
        );

        log.info("Sending prompt to ChatGPT for niche {}: {}", niche.getId(), promptData.prompt());
        log.debug("ChatGPT payload: {}", payload);

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        log.info("ChatGPT raw response: {}", response);

        if (response == null || response.choices().isEmpty()) {
            log.warn("ChatGPT returned no choices for niche {}", niche.getId());
            return List.of();
        }
        String content = response.choices().get(0).message().content();
        log.info("ChatGPT content: {}", content);
        try {
            List<CreateHypothesisRequest> parsed = parseContent(content, niche, promptData);
            log.info("Parsed hypotheses: {}", parsed);
            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            try {
                // ChatGPT may return JSON with escaped quotes. Attempt to unescape them and parse again.
                String unescaped = content.replace("\\\"", "\"");
                List<CreateHypothesisRequest> parsed = parseContent(unescaped, niche, promptData);
                log.info("Parsed hypotheses after unescaping: {}", parsed);
                return parsed;
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT response: {}", content, ex);
                throw new RuntimeException("Failed to parse ChatGPT response", ex);
            }
        }
    }

    private PromptData buildPrompt(MarketNiche niche, int quantity) {
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
        var attrs = attributeRepository.findByEntity_Name("hypothesis");
        var descriptionIds = new java.util.ArrayList<Long>();
        var descriptions = attrs.stream()
                .map(attr -> {
                    var opt = descriptionRepository.findByAttribute_IdAndActiveTrue(attr.getId());
                    return opt.map(d -> {
                        descriptionIds.add(d.getId());
                        return Map.entry(attr.getName(), d.getDescription());
                    });
                })
                .flatMap(java.util.Optional::stream)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!descriptions.isEmpty()) {
            sb.append("Cada objeto deve conter as chaves: ")
                    .append(descriptions.keySet().stream().map(n -> "\\\"" + n + "\\\"")
                            .collect(java.util.stream.Collectors.joining(", ")))
                    .append(". ");
            descriptions.forEach((name, desc) ->
                    sb.append("Campo \\\"" + name + "\\\": " + desc + ". "));
        } else {
            sb.append("Cada objeto deve conter as chaves: \"title\", \"promise\", \"problem\", \"persona\", \"mechanism\", \"uniqueMechanism\", \"successRule\", \"offerType\", \"price\". ");
        }
        sb.append("O campo \"offerType\" deve ser \"LEAD\" ou \"TRIPWIRE\". ");
        sb.append("O campo \"price\" deve ser um número. ");
        sb.append("Retorne apenas um array JSON com esses objetos, sem texto adicional.");
        return new PromptData(sb.toString(), descriptionIds);
    }

    private record ChatCompletionResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}

    private List<CreateHypothesisRequest> parseContent(String content, MarketNiche niche, PromptData data) throws Exception {
        CreateHypothesisRequest[] arr = objectMapper.readValue(content, CreateHypothesisRequest[].class);
        for (CreateHypothesisRequest req : arr) {
            req.setMarketNicheId(niche.getId());
            req.setPrompt(data.prompt());
            req.setModel(model);
            req.setPromptAttributeDescriptionIds(data.descriptionIds());
        }
        return Arrays.asList(arr);
    }

    private record PromptData(String prompt, List<Long> descriptionIds) {}
}
