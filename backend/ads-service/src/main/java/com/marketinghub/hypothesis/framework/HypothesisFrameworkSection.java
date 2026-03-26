package com.marketinghub.hypothesis.framework;

import org.springframework.util.StringUtils;

public enum HypothesisFrameworkSection {
    PAIN("pain"),
    RESULT("result"),
    MECHANISM("mechanism"),
    PROOF("proof"),
    OFFER("offer");

    private final String path;

    HypothesisFrameworkSection(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static HypothesisFrameworkSection fromPath(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("Section is required");
        }
        String normalized = raw.trim().toLowerCase();
        for (HypothesisFrameworkSection section : values()) {
            if (section.path.equals(normalized)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown framework section: " + raw);
    }
}
