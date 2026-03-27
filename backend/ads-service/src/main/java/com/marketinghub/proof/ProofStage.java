package com.marketinghub.proof;

import java.util.Locale;

/**
 * Funnel stage where a proof artifact is used.
 */
public enum ProofStage {
    AD("Anúncio"),
    LANDING("Landing"),
    SAMPLE("Amostra"),
    SALES("Oferta/Venda");

    private final String displayName;

    ProofStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProofStage fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return SAMPLE;
        }
        try {
            return ProofStage.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SAMPLE;
        }
    }
}
