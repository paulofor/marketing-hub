package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.worker.targetingrequest.TargetingAudienceType;
import com.marketinghub.worker.targetingrequest.TargetingCandidateSuggestion;
import com.marketinghub.worker.targetingrequest.TargetingCandidateType;
import com.marketinghub.worker.targetingrequest.TargetingRequestChatGptClient;
import com.marketinghub.worker.targetingrequest.TargetingRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Builds the initial seed definition for the workflow (AI job).
 */
@Component
public class AdSetSeedPlanner {
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^a-zA-Zá-úÁ-Ú0-9]+");
    private static final Set<String> STOPWORDS = Set.of(
            "marketing", "ads", "campanha", "teste", "oferta", "plano", "experimento"
    );
    private final ObjectMapper objectMapper;
    private final TargetingRequestChatGptClient targetingRequestChatGptClient;

    public AdSetSeedPlanner(ObjectMapper objectMapper,
                            TargetingRequestChatGptClient targetingRequestChatGptClient) {
        this.objectMapper = objectMapper;
        this.targetingRequestChatGptClient = targetingRequestChatGptClient;
    }

    public ObjectNode plan(JsonNode payload) {
        ObjectNode result = objectMapper.createObjectNode();
        String locale = text(payload, "defaultLocale", "pt_BR");
        String keyword = chooseKeyword(payload);
        ObjectNode aiCandidates = buildAiCandidates(payload, locale);

        result.put("seedKeyword", keyword);
        result.put("seedLocale", locale);
        result.set("interests", ensureNotEmpty(aiCandidates.path("interests"), buildSearchTerms(keyword)));
        result.set("work_positions", ensureNotEmpty(aiCandidates.path("work_positions"), buildPositionQueries()));
        result.set("behaviors", ensureNotEmpty(aiCandidates.path("behaviors"), buildBehaviorQueries()));
        result.set("searchTerms", ensureNotEmpty(result.path("interests"), buildSearchTerms(keyword)));
        result.set("positionQueries", ensureNotEmpty(result.path("work_positions"), buildPositionQueries()));
        result.put("notes", text(aiCandidates, "notes", "Seed derivado do contexto do experimento e do nicho"));
        return result;
    }

    private ObjectNode buildAiCandidates(JsonNode payload, String locale) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set("interests", objectMapper.createArrayNode());
        result.set("work_positions", objectMapper.createArrayNode());
        result.set("behaviors", objectMapper.createArrayNode());
        result.put("notes", "Seed derivado por heurística local");

        try {
            TargetingRequestDto request = new TargetingRequestDto(
                    UUID.randomUUID(),
                    buildDescription(payload),
                    locale,
                    "BR",
                    TargetingAudienceType.PROSPECT,
                    null,
                    null
            );
            for (TargetingCandidateSuggestion candidate : targetingRequestChatGptClient.generateCandidates(request)) {
                if (candidate == null || !StringUtils.hasText(candidate.seed())) {
                    continue;
                }
                String normalized = candidate.seed().trim();
                if (candidate.tipo() == TargetingCandidateType.BEHAVIOR) {
                    appendDistinct((ArrayNode) result.path("behaviors"), normalized, 5);
                } else if (candidate.tipo() == TargetingCandidateType.WORK_POSITION) {
                    appendDistinct((ArrayNode) result.path("work_positions"), normalized, 5);
                } else {
                    appendDistinct((ArrayNode) result.path("interests"), normalized, 5);
                }
            }
            if (!result.path("interests").isEmpty() || !result.path("work_positions").isEmpty() || !result.path("behaviors").isEmpty()) {
                result.put("notes", "Seed derivado via ChatGPT com candidatos por tipo");
            }
        } catch (Exception ignored) {
            // fallback para heurística local quando o ChatGPT falhar.
        }
        return result;
    }

    private String buildDescription(JsonNode payload) {
        return String.join("\n", Arrays.asList(
                "Produto: " + text(payload, "experimentName", "Marketing Hub"),
                "Hipótese: " + text(payload, "experimentHypothesis", ""),
                "Nicho: " + text(payload, "nicheName", ""),
                "Segmentação: " + text(payload, "nicheSegmentation", ""),
                "Persona: " + text(payload, "hypothesisPersona", "")
        ));
    }

    private void appendDistinct(ArrayNode array, String value, int max) {
        if (!StringUtils.hasText(value) || array == null || array.size() >= max) {
            return;
        }
        for (JsonNode item : array) {
            if (value.equalsIgnoreCase(item.asText())) {
                return;
            }
        }
        array.add(value);
    }

    private ArrayNode ensureNotEmpty(JsonNode node, ArrayNode fallback) {
        if (node != null && node.isArray() && !node.isEmpty()) {
            ArrayNode copy = objectMapper.createArrayNode();
            node.forEach(copy::add);
            return copy;
        }
        return fallback;
    }

    private ArrayNode buildSearchTerms(String keyword) {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(keyword);
        array.add(keyword + " software");
        return array;
    }

    private ArrayNode buildPositionQueries() {
        ArrayNode array = objectMapper.createArrayNode();
        array.add("social media");
        array.add("social media manager");
        array.add("social media strategist");
        return array;
    }

    private ArrayNode buildBehaviorQueries() {
        ArrayNode array = objectMapper.createArrayNode();
        array.add("compradores engajados");
        array.add("small business owners");
        return array;
    }

    private String chooseKeyword(JsonNode payload) {
        String nicheName = text(payload, "nicheName", null);
        if (StringUtils.hasText(nicheName)) {
            return nicheName.trim();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (String field : Arrays.asList("experimentName", "hypothesisTitle", "nicheName", "experimentHypothesis")) {
            String value = text(payload, field, null);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            Arrays.stream(TOKEN_SPLITTER.split(value.toLowerCase(Locale.ROOT)))
                    .filter(token -> token.length() > 3)
                    .filter(token -> !STOPWORDS.contains(token))
                    .forEach(tokens::add);
        }
        if (tokens.isEmpty()) {
            return "social media";
        }
        return tokens.iterator().next();
    }

    private String text(JsonNode node, String field, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : defaultValue;
    }
}
