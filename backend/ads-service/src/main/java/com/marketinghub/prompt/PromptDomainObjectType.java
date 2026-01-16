package com.marketinghub.prompt;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public enum PromptDomainObjectType {
    DETAILED_DESCRIPTION("descricao-detalhada", "Descrição detalhada", "detailedDescription"),
    DIFFERENTIATED_TECHNOLOGY("tecnologia-diferenciada", "Tecnologia diferenciada", "technology"),
    NICHE("nicho", "Nicho", "niche"),
    JOURNEY("jornada", "Jornada", "journey"),
    HYPOTHESIS("hipotese", "Hipótese", "hypothesis");

    private final String slug;
    private final String label;
    private final String contextKey;

    PromptDomainObjectType(String slug, String label, String contextKey) {
        this.slug = slug;
        this.label = label;
        this.contextKey = contextKey;
    }

    public String getSlug() {
        return slug;
    }

    public String getLabel() {
        return label;
    }

    public String getContextKey() {
        return contextKey;
    }

    public static PromptDomainObjectType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized)
                        || item.slug.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported prompt object: " + value));
    }

    public static boolean equals(PromptDomainObjectType type, String value) {
        if (type == null || value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return type.name().equalsIgnoreCase(normalized) || type.slug.equalsIgnoreCase(normalized);
    }

    public static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    public static boolean exists(String value) {
        try {
            return fromValue(value) != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static PromptDomainObjectType[] copyOf(PromptDomainObjectType[] original) {
        if (original == null) {
            return new PromptDomainObjectType[0];
        }
        return Arrays.stream(original).filter(Objects::nonNull).toArray(PromptDomainObjectType[]::new);
    }
}
