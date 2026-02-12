package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdSetSpecBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AdSetSpecBuilder builder = new AdSetSpecBuilder(mapper);

    @Test
    void shouldSendBehaviorSuggestionsToBehaviorBlock() {
        ObjectNode payload = mapper.createObjectNode();
        payload.set("seed", buildSeed("123", "Seed"));
        payload.set("positions", wrapItems(mapper.createArrayNode()));

        ArrayNode suggestionItems = mapper.createArrayNode();
        suggestionItems.add(buildSuggestion("600438", "Mobile users", new String[]{"Behaviors", "Mobile Device User"}));
        suggestionItems.add(buildSuggestion("700000", "Design lovers", new String[]{"Interests", "Business"}));
        payload.set("suggestions", wrapItems(suggestionItems));

        ObjectNode result = builder.build(payload);
        JsonNode designersFlexible = result.path("specs").get(0).path("targetingSpec").path("flexible_spec");
        JsonNode smbFlexible = result.path("specs").get(2).path("targetingSpec").path("flexible_spec");

        JsonNode designerInterestBlock = findBlock(designersFlexible, "interests");
        assertThat(designerInterestBlock).isNotNull();
        List<String> designerInterestIds = extractIds(designerInterestBlock.path("interests"));
        assertThat(designerInterestIds).contains("700000").doesNotContain("600438");

        JsonNode behaviorBlock = findBlock(smbFlexible, "behaviors");
        assertThat(behaviorBlock).isNotNull();
        List<String> behaviorIds = extractIds(behaviorBlock.path("behaviors"));
        assertThat(behaviorIds).contains("600438");
    }

    @Test
    void shouldFallbackToSeedWhenNoInterestSuggestionsAreAvailable() {
        ObjectNode payload = mapper.createObjectNode();
        payload.set("seed", buildSeed("321", "Seed Fallback"));
        payload.set("positions", wrapItems(mapper.createArrayNode()));

        ArrayNode suggestionItems = mapper.createArrayNode();
        suggestionItems.add(buildSuggestion("800000", "Travelers", new String[]{"Behaviors", "Travel"}));
        payload.set("suggestions", wrapItems(suggestionItems));

        ObjectNode result = builder.build(payload);
        JsonNode flexibleSpec = result.path("specs").get(0).path("targetingSpec").path("flexible_spec");
        JsonNode interestBlock = findBlock(flexibleSpec, "interests");

        assertThat(interestBlock).isNotNull();
        List<String> interestIds = extractIds(interestBlock.path("interests"));
        assertThat(interestIds).contains("321");
    }

    private ObjectNode buildSeed(String id, String name) {
        ObjectNode seed = mapper.createObjectNode();
        seed.put("interestId", id);
        seed.put("interestName", name);
        return seed;
    }

    private ObjectNode buildSuggestion(String id, String name, String[] pathEntries) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", id);
        node.put("name", name);
        ArrayNode path = mapper.createArrayNode();
        for (String entry : pathEntries) {
            path.add(entry);
        }
        node.set("path", path);
        return node;
    }

    private ObjectNode wrapItems(ArrayNode items) {
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.set("items", items);
        return wrapper;
    }

    private JsonNode findBlock(JsonNode flexibleSpec, String field) {
        if (flexibleSpec == null || !flexibleSpec.isArray()) {
            return null;
        }
        for (JsonNode node : flexibleSpec) {
            if (node.has(field)) {
                return node;
            }
        }
        return null;
    }

    private List<String> extractIds(JsonNode arrayNode) {
        List<String> ids = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return ids;
        }
        for (JsonNode item : arrayNode) {
            ids.add(item.path("id").asText());
        }
        return ids;
    }
}
