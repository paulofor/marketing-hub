package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic builder that converts Meta suggestions into the three targeting specs.
 */
@Component
public class AdSetSpecBuilder {
    private static final Set<String> DESIGN_KEYWORDS = Set.of("design", "designer", "foto", "imagem", "creative", "photoshop", "canva");
    private static final Set<String> MARKETING_KEYWORDS = Set.of("marketing", "social", "midia", "conteudo", "publicidad", "trafego");

    private final ObjectMapper mapper;

    public AdSetSpecBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectNode build(JsonNode payload) {
        List<SuggestionItem> suggestions = parseSuggestions(payload.path("suggestions"));
        List<SuggestionItem> positions = parsePositions(payload.path("positions"));
        SuggestionItem seed = parseSeed(payload.path("seed"));
        List<SuggestionItem> behaviors = suggestions.stream()
                .filter(item -> item.type() == SuggestionItemType.BEHAVIOR)
                .collect(Collectors.toList());
        List<SuggestionItem> interests = suggestions.stream()
                .filter(item -> item.type() == SuggestionItemType.INTEREST)
                .collect(Collectors.toList());

        ObjectNode result = mapper.createObjectNode();
        ArrayNode specs = mapper.createArrayNode();
        specs.add(buildDesignSpec(interests, positions, seed));
        specs.add(buildMarketingSpec(interests, positions, seed));
        specs.add(buildSmbSpec(interests, behaviors, seed));
        result.set("specs", specs);
        return result;
    }

    private ObjectNode buildDesignSpec(List<SuggestionItem> interests,
                                       List<SuggestionItem> positions,
                                       SuggestionItem seed) {
        List<SuggestionItem> designInterests = selectByKeyword(interests, DESIGN_KEYWORDS, 4);
        designInterests = ensureInterests(designInterests, interests, seed, 4);
        ObjectNode targeting = baseTargeting(18, 45);
        ArrayNode flexible = mapper.createArrayNode();
        JsonNode interestBlock = buildInterestBlock(designInterests);
        if (interestBlock != null) {
            flexible.add(interestBlock);
        }
        JsonNode positionBlock = buildPositionBlock(positions);
        if (positionBlock != null) {
            flexible.add(positionBlock);
        }
        targeting.set("flexible_spec", flexible);
        return specNode("DESIGNERS", "Designers / Edição", targeting);
    }

    private ObjectNode buildMarketingSpec(List<SuggestionItem> interests,
                                          List<SuggestionItem> positions,
                                          SuggestionItem seed) {
        List<SuggestionItem> marketingInterests = selectByKeyword(interests, MARKETING_KEYWORDS, 4);
        marketingInterests = ensureInterests(marketingInterests, interests, seed, 4);
        ObjectNode targeting = baseTargeting(20, 55);
        ArrayNode flexible = mapper.createArrayNode();
        JsonNode interestBlock = buildInterestBlock(marketingInterests);
        if (interestBlock != null) {
            flexible.add(interestBlock);
        }
        JsonNode positionBlock = buildPositionBlock(positions);
        if (positionBlock != null) {
            flexible.add(positionBlock);
        }
        targeting.set("flexible_spec", flexible);
        return specNode("MARKETING", "Gestores de Social Media", targeting);
    }

    private ObjectNode buildSmbSpec(List<SuggestionItem> interests,
                                    List<SuggestionItem> behaviors,
                                    SuggestionItem seed) {
        ObjectNode targeting = baseTargeting(22, 60);
        ArrayNode flexible = mapper.createArrayNode();
        JsonNode behaviorBlock = buildBehaviorBlock(behaviors);
        if (behaviorBlock != null) {
            flexible.add(behaviorBlock);
        }
        List<SuggestionItem> smbInterests = new ArrayList<>();
        addIfAbsent(smbInterests, seed);
        interests.stream().limit(4).forEach(item -> addIfAbsent(smbInterests, item));
        JsonNode interestBlock = buildInterestBlock(smbInterests);
        if (interestBlock != null) {
            flexible.add(interestBlock);
        }
        targeting.set("flexible_spec", flexible);
        return specNode("SMB", "Donos de negócio", targeting);
    }

    private ObjectNode specNode(String slot, String label, ObjectNode targeting) {
        ObjectNode spec = mapper.createObjectNode();
        spec.put("slot", slot);
        spec.put("label", label);
        spec.put("ageMin", targeting.path("age_min").asInt());
        spec.put("ageMax", targeting.path("age_max").asInt());
        spec.set("targetingSpec", targeting);
        return spec;
    }

    private ObjectNode baseTargeting(int ageMin, int ageMax) {
        ObjectNode targeting = mapper.createObjectNode();
        ObjectNode geo = mapper.createObjectNode();
        ArrayNode countries = mapper.createArrayNode();
        countries.add("BR");
        geo.set("countries", countries);
        targeting.set("geo_locations", geo);
        targeting.put("age_min", ageMin);
        targeting.put("age_max", ageMax);
        return targeting;
    }

    private JsonNode buildInterestBlock(List<SuggestionItem> interests) {
        if (CollectionUtils.isEmpty(interests)) {
            return null;
        }
        ArrayNode array = mapper.createArrayNode();
        interests.stream()
                .filter(Objects::nonNull)
                .limit(5)
                .forEach(item -> array.add(toTargetingOption(item)));
        if (array.isEmpty()) {
            return null;
        }
        ObjectNode block = mapper.createObjectNode();
        block.set("interests", array);
        return block;
    }

    private JsonNode buildPositionBlock(List<SuggestionItem> positions) {
        if (CollectionUtils.isEmpty(positions)) {
            return null;
        }
        ArrayNode array = mapper.createArrayNode();
        positions.stream().limit(3).forEach(item -> array.add(toTargetingOption(item)));
        ObjectNode block = mapper.createObjectNode();
        block.set("work_positions", array);
        return block;
    }

    private JsonNode buildBehaviorBlock(List<SuggestionItem> behaviors) {
        if (CollectionUtils.isEmpty(behaviors)) {
            return null;
        }
        ArrayNode array = mapper.createArrayNode();
        behaviors.stream()
                .filter(Objects::nonNull)
                .limit(3)
                .forEach(item -> array.add(toTargetingOption(item)));
        if (array.isEmpty()) {
            return null;
        }
        ObjectNode block = mapper.createObjectNode();
        block.set("behaviors", array);
        return block;
    }

    private ObjectNode toTargetingOption(SuggestionItem item) {
        ObjectNode option = mapper.createObjectNode();
        option.put("id", item.id());
        option.put("name", item.name());
        return option;
    }

    private List<SuggestionItem> selectByKeyword(List<SuggestionItem> source, Set<String> keywords, int limit) {
        return source.stream()
                .filter(item -> containsKeyword(item.name(), keywords))
                .sorted(Comparator.comparing(SuggestionItem::audienceSize, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private boolean containsKeyword(String value, Set<String> keywords) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(lower::contains);
    }

    private List<SuggestionItem> parseSuggestions(JsonNode node) {
        List<SuggestionItem> items = new ArrayList<>();
        JsonNode array = node;
        if (node != null && node.isObject()) {
            array = node.path("items");
        }
        if (array == null || !array.isArray()) {
            return items;
        }
        array.forEach(entry -> {
            String id = entry.path("id").asText(null);
            String name = entry.path("name").asText(null);
            Long audience = entry.has("audienceSize") && entry.get("audienceSize").isNumber()
                    ? entry.get("audienceSize").longValue()
                    : null;
            if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
                items.add(new SuggestionItem(id.trim(), name.trim(), resolveType(entry), audience));
            }
        });
        return items;
    }

    private List<SuggestionItem> parsePositions(JsonNode node) {
        List<SuggestionItem> items = new ArrayList<>();
        JsonNode array = node;
        if (node != null && node.isObject()) {
            array = node.path("items");
        }
        if (array == null || !array.isArray()) {
            return items;
        }
        array.forEach(entry -> {
            String id = entry.path("id").asText(null);
            String name = entry.path("name").asText(null);
            if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
                items.add(new SuggestionItem(id.trim(), name.trim(), SuggestionItemType.WORK_POSITION, null));
            }
        });
        return items;
    }

    private SuggestionItem parseSeed(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String id = node.path("interestId").asText(null);
        String name = node.path("interestName").asText(null);
        if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) {
            return null;
        }
        return new SuggestionItem(id.trim(), name.trim(), SuggestionItemType.INTEREST, null);
    }

    private List<SuggestionItem> ensureInterests(List<SuggestionItem> primary,
                                                 List<SuggestionItem> fallback,
                                                 SuggestionItem seed,
                                                 int limit) {
        List<SuggestionItem> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(primary)) {
            result.addAll(primary);
        }
        if (result.isEmpty() && !CollectionUtils.isEmpty(fallback)) {
            result.addAll(fallback.stream().limit(limit).collect(Collectors.toList()));
        }
        if (seed != null && result.stream().noneMatch(item -> item.id().equals(seed.id()))) {
            result.add(0, seed);
        }
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private void addIfAbsent(List<SuggestionItem> target, SuggestionItem candidate) {
        if (candidate == null) {
            return;
        }
        boolean exists = target.stream().anyMatch(item -> item.id().equals(candidate.id()));
        if (!exists) {
            target.add(candidate);
        }
    }

    private SuggestionItemType resolveType(JsonNode entry) {
        if (entry == null || entry.isNull()) {
            return SuggestionItemType.INTEREST;
        }
        String rawType = entry.path("type").asText(null);
        if (StringUtils.hasText(rawType)) {
            String normalized = rawType.trim().toUpperCase(Locale.ROOT);
            if (normalized.contains("BEHAVIOR")) {
                return SuggestionItemType.BEHAVIOR;
            }
            if (normalized.contains("DEMOGRAPH")) {
                return SuggestionItemType.DEMOGRAPHIC;
            }
        }
        JsonNode pathNode = entry.get("path");
        if (pathNode != null && pathNode.isArray() && pathNode.size() > 0) {
            String category = pathNode.get(0).asText("");
            if ("Behaviors".equalsIgnoreCase(category)) {
                return SuggestionItemType.BEHAVIOR;
            }
            if ("Demographics".equalsIgnoreCase(category)) {
                return SuggestionItemType.DEMOGRAPHIC;
            }
        }
        return SuggestionItemType.INTEREST;
    }

    private enum SuggestionItemType {
        INTEREST,
        BEHAVIOR,
        DEMOGRAPHIC,
        WORK_POSITION
    }

    private record SuggestionItem(String id, String name, SuggestionItemType type, Long audienceSize) {
    }
}
