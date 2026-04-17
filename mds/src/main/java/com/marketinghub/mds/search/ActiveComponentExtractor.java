package com.marketinghub.mds.search;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ActiveComponentExtractor {
    private static final List<String> COMPONENT_HINTS = List.of(
            "calorie deficit",
            "protein intake",
            "resistance training",
            "strength training",
            "aerobic exercise",
            "sleep quality",
            "stress management",
            "fiber intake",
            "hydration",
            "behavioral adherence",
            "meal timing"
    );

    private static final Set<String> STOPWORDS = Set.of(
            "with", "from", "that", "this", "were", "have", "into", "between", "through",
            "loss", "weight", "result", "results", "problem", "desired", "outcome", "market",
            "study", "studies", "effect", "effects", "group", "groups", "patient", "patients"
    );

    public List<String> extract(ScreenedEvidence evidence) {
        String content = ((safe(evidence.title()) + " " + safe(evidence.abstractText()))).toLowerCase(Locale.ROOT);
        Set<String> components = new LinkedHashSet<>();

        for (String hint : COMPONENT_HINTS) {
            if (content.contains(hint)) {
                components.add(hint);
            }
        }

        if (!components.isEmpty()) {
            return new ArrayList<>(components);
        }

        for (String token : content.replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (token.length() < 6 || STOPWORDS.contains(token)) {
                continue;
            }
            components.add(token);
            if (components.size() >= 3) {
                break;
            }
        }

        return new ArrayList<>(components);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
