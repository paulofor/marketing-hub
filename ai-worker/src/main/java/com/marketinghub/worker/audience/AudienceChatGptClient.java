package com.marketinghub.worker.audience;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.audience.AudienceSource;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wrapper do ChatGPT para geração de públicos de Meta Ads.
 */
@Component
public class AudienceChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(AudienceChatGptClient.class);
    private static final String DOMAIN = "AUDIENCE_SEGMENT";

    public AudienceChatGptClient(WebClient.Builder builder,
                                 ObjectMapper objectMapper,
                                 @Value("${openai.api-key:}") String apiKey,
                                 @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                 @Value("${openai.model:gpt-3.5-turbo}") String model,
                                 AiGenerationRecorder generationRecorder) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.generationRecorder = generationRecorder;
    }

    public List<CreateAudienceRequest> generateAudiences(MarketNiche niche,
                                                         List<Hypothesis> hypotheses,
                                                         int quantity) {
        PromptData promptData = buildPrompt(niche, hypotheses, quantity);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em marketing e segmentação para anúncios da Meta."),
                OpenAiRequestUtils.message("user", promptData.prompt())
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Sending prompt to ChatGPT for niche {}: {}", niche.getId(), promptData.prompt());
        log.debug("ChatGPT payload: {}", payload);

        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null) {
            log.warn("ChatGPT returned no choices for niche {}", niche.getId());
            return List.of();
        }

        if (response.hasError()) {
            throw new RuntimeException("OpenAI error: " + response.errorMessage());
        }

        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                niche != null ? String.valueOf(niche.getId()) : null,
                promptData.prompt(),
                content,
                model,
                response.usage());
        if (content == null || content.isBlank()) {
            log.warn("ChatGPT returned empty content for niche {}", niche.getId());
            return List.of();
        }
        log.info("ChatGPT content: {}", content);

        try {
            return parseContent(content, niche, promptData, quantity);
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            try {
                String unescaped = content.replace("\\\"", "\"");
                return parseContent(unescaped, niche, promptData, quantity);
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT response: {}", content, ex);
                throw new RuntimeException("Failed to parse ChatGPT response", ex);
            }
        }
    }

    private PromptData buildPrompt(MarketNiche niche, List<Hypothesis> hypotheses, int quantity) {
        Set<UUID> hypothesisIds = hypotheses.stream()
                .map(Hypothesis::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        StringBuilder sb = new StringBuilder();
        sb.append("Gere ").append(quantity).append(" públicos para campanhas de Meta Ads em formato JSON. ");
        sb.append("Cada objeto deve conter as chaves \"name\", \"description\" e \"hypothesisId\". ");
        sb.append("Use null em \"hypothesisId\" quando o público for geral do nicho.\n");
        sb.append("Descreva cada público com interesses, comportamentos ou segmentações sugeridas em até três frases.\n");
        sb.append("Nicho:\n");
        appendField(sb, "Nome", niche.getName());
        appendField(sb, "Descrição", niche.getDescription());
        appendField(sb, "Segmentação base", niche.getBaseSegmentation());
        appendField(sb, "Interesses", niche.getInterests());
        appendField(sb, "Filtros demográficos", niche.getDemographicFilters());
        appendField(sb, "Dicas extras", niche.getExtraTips());

        if (!hypotheses.isEmpty()) {
            sb.append("Hipóteses disponíveis com seus identificadores:\n");
            for (Hypothesis h : hypotheses) {
                sb.append("- ID: ").append(h.getId()).append(" | Título: ").append(truncate(h.getTitle(), 140)).append("\n");
                appendIndentedField(sb, "Promessa", h.getPromise());
                appendIndentedField(sb, "Persona", h.getPersona());
                appendIndentedField(sb, "Mecanismo", h.getMechanism());
                appendIndentedField(sb, "Mecanismo único", h.getUniqueMechanism());
            }
            sb.append("Use o campo hypothesisId com um dos IDs listados quando o público for específico de uma hipótese.\n");
        } else {
            sb.append("Não há hipóteses cadastradas. Gere públicos gerais do nicho e defina hypothesisId como null.\n");
        }
        sb.append("Retorne apenas um array JSON com os objetos, sem texto adicional.");
        return new PromptData(sb.toString(), hypothesisIds);
    }

    private List<CreateAudienceRequest> parseContent(String content,
                                                     MarketNiche niche,
                                                     PromptData data,
                                                     int quantity) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        JsonNode array = root;
        if (root.isObject()) {
            if (root.has("audiences")) {
                array = root.get("audiences");
            } else if (root.has("items")) {
                array = root.get("items");
            }
        }
        if (!array.isArray()) {
            throw new IllegalArgumentException("Expected JSON array with audiences");
        }
        List<CreateAudienceRequest> result = new ArrayList<>();
        for (JsonNode node : array) {
            CreateAudienceRequest req = new CreateAudienceRequest();
            req.setName(asText(node, "name"));
            req.setDescription(asText(node, "description"));
            String hypothesisIdText = asText(node, "hypothesisId");
            if (hypothesisIdText != null && !hypothesisIdText.isBlank() && !"null".equalsIgnoreCase(hypothesisIdText)) {
                try {
                    UUID hypothesisId = UUID.fromString(hypothesisIdText.trim());
                    if (data.hypothesisIds().contains(hypothesisId)) {
                        req.setHypothesisId(hypothesisId);
                    } else {
                        log.warn("Ignoring hypothesisId {} not linked to niche {}", hypothesisId, niche.getId());
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid hypothesisId '{}' for niche {}", hypothesisIdText, niche.getId());
                }
            }
            req.setMarketNicheId(niche.getId());
            req.setPrompt(data.prompt());
            req.setModel(model);
            req.setSource(AudienceSource.AI);
            result.add(req);
            if (result.size() >= quantity) {
                break;
            }
        }
        return result;
    }

    private static String asText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(truncate(value, 500)).append("\n");
        }
    }

    private static void appendIndentedField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("  ").append(label).append(": ").append(truncate(value, 300)).append("\n");
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private record PromptData(String prompt, Set<UUID> hypothesisIds) {}
}
