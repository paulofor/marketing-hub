package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
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

    public AdSetSeedPlanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode plan(JsonNode payload) {
        ObjectNode result = objectMapper.createObjectNode();
        String locale = text(payload, "defaultLocale", "pt_BR");
        String keyword = chooseKeyword(payload);
        result.put("seedKeyword", keyword);
        result.put("seedLocale", locale);
        result.set("searchTerms", buildSearchTerms(keyword));
        result.set("positionQueries", buildPositionQueries());
        result.put("notes", "Seed derivado do contexto do experimento e do nicho");
        return result;
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

    private String chooseKeyword(JsonNode payload) {
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
