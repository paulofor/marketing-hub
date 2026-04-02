package com.marketinghub.experiment.pipeline.ads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Utility responsible for translating the structured pipeline outputs into
 * ad creative plans.
 */
public class ExperimentPipelineAdExtractor {
    private final ObjectMapper objectMapper;

    public ExperimentPipelineAdExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PipelineAdCreativePlan> extract(Experiment experiment) {
        if (experiment == null || !StringUtils.hasText(experiment.getAdCopy())) {
            return List.of();
        }
        JsonNode adCopyRoot = parse(experiment.getAdCopy());
        if (adCopyRoot == null) {
            return List.of();
        }
        JsonNode scope = adCopyRoot.hasNonNull("adCopy") ? adCopyRoot.get("adCopy") : adCopyRoot;
        JsonNode variants = scope != null ? scope.get("primaryTextVariants") : null;
        if (variants == null || !variants.isArray()) {
            return List.of();
        }
        List<PipelineImageBriefing> orderedBriefings = new ArrayList<>();
        Map<String, PipelineImageBriefing> briefingsByKey = extractImageBriefings(experiment.getAdImageBriefing(), orderedBriefings);
        List<PipelineAdCreativePlan> plans = new ArrayList<>();
        int index = 0;
        for (JsonNode variant : variants) {
            PipelineAdCreativePlan plan = toPlan(variant, index++, briefingsByKey, orderedBriefings);
            if (plan != null) {
                plans.add(plan);
            }
        }
        return plans;
    }

    private JsonNode parse(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            return null;
        }
    }

    private PipelineAdCreativePlan toPlan(JsonNode variant,
                                          int index,
                                          Map<String, PipelineImageBriefing> briefingsByKey,
                                          List<PipelineImageBriefing> orderedBriefings) {
        if (variant == null || !variant.isObject()) {
            return null;
        }
        String label = text(variant, "label", "variant", "mustMatchAdVariant", "openingHookType");
        String normalizedLabel = normalize(label);
        String fallbackLabel = "variant-" + (index + 1);
        String placementHint = text(variant, "placementHint");
        if (!StringUtils.hasText(placementHint)) {
            placementHint = "feed";
        }
        String headline = text(variant, "headline", "title");
        String primaryText = resolvePrimaryText(variant);
        String description = text(variant, "description", "subtitle");
        String ctaText = text(variant, "ctaText", "cta");
        PipelineImageBriefing briefing = pickBriefing(normalizedLabel, briefingsByKey, orderedBriefings);
        String format = resolveFormat(placementHint, briefing);
        return new PipelineAdCreativePlan(
                StringUtils.hasText(label) ? label : fallbackLabel,
                placementHint,
                headline,
                primaryText,
                description,
                ctaText,
                format,
                briefing);
    }

    private String resolveFormat(String placementHint, PipelineImageBriefing briefing) {
        String normalizedPlacement = placementHint != null ? placementHint.toLowerCase(Locale.ROOT) : "";
        String assetType = briefing != null ? briefing.assetType() : null;
        if ((assetType != null && assetType.toLowerCase(Locale.ROOT).contains("story"))
                || normalizedPlacement.contains("story")
                || normalizedPlacement.contains("reel")) {
            return "STORY";
        }
        return "LINK";
    }

    private String resolvePrimaryText(JsonNode variant) {
        String direct = text(variant, "primaryText", "text");
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        JsonNode lengths = variant.get("lengthVariants");
        if (lengths != null && lengths.isObject()) {
            String[] preferredOrder = {"media", "curta", "longa"};
            for (String key : preferredOrder) {
                String value = text(lengths, key);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private Map<String, PipelineImageBriefing> extractImageBriefings(String rawJson,
                                                                     List<PipelineImageBriefing> orderedBriefings) {
        if (!StringUtils.hasText(rawJson)) {
            return Collections.emptyMap();
        }
        JsonNode root = parse(rawJson);
        if (root == null) {
            return Collections.emptyMap();
        }
        JsonNode scope = root.hasNonNull("adImageBriefing") ? root.get("adImageBriefing") : root;
        JsonNode briefings = scope != null ? scope.get("briefings") : null;
        if (briefings == null || !briefings.isArray()) {
            return Collections.emptyMap();
        }
        Map<String, PipelineImageBriefing> byKey = new LinkedHashMap<>();
        for (JsonNode node : briefings) {
            if (node == null || !node.isObject()) {
                continue;
            }
            PipelineImageBriefing briefing = new PipelineImageBriefing(
                    text(node, "mustMatchAdVariant", "visualAngle"),
                    text(node, "visualAngle"),
                    text(node, "assetType"),
                    readInteger(node.get("imageTextMaxWords")),
                    text(node, "visualBriefing"),
                    text(node, "hierarchy"),
                    text(node, "formatByPlacement"),
                    text(node, "safeMargins"),
                    text(node, "complianceNotes"),
                    text(node, "messageMatchNotes"),
                    readKeywords(node.get("supportingKeywords")));
            orderedBriefings.add(briefing);
            String normalized = normalize(briefing.mustMatchAdVariant());
            if (normalized != null) {
                byKey.putIfAbsent(normalized, briefing);
            }
        }
        return byKey;
    }

    private List<String> readKeywords(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> keywords = new ArrayList<>();
        for (JsonNode child : node) {
            String value = child != null ? child.asText(null) : null;
            if (StringUtils.hasText(value)) {
                keywords.add(value.trim());
            }
        }
        return keywords;
    }

    private PipelineImageBriefing pickBriefing(String normalizedLabel,
                                               Map<String, PipelineImageBriefing> byKey,
                                               List<PipelineImageBriefing> ordered) {
        if (normalizedLabel != null && byKey.containsKey(normalizedLabel)) {
            return byKey.get(normalizedLabel);
        }
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    private Integer readInteger(JsonNode node) {
        if (node == null || !node.canConvertToInt()) {
            return null;
        }
        return node.asInt();
    }

    private String text(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isTextual()) {
                String text = value.asText();
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String noDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String sanitized = noDiacritics.replaceAll("[^A-Za-z0-9]+", "");
        return sanitized.isEmpty() ? null : sanitized.toLowerCase(Locale.ROOT);
    }
}
