package com.marketinghub.proof;

import java.util.Locale;

/**
 * Status of a proof artifact lifecycle.
 */
public enum ProofStatus {
    DRAFT,
    APPROVED,
    ARCHIVED;

    public static ProofStatus fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return DRAFT;
        }
        try {
            return ProofStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DRAFT;
        }
    }
}
