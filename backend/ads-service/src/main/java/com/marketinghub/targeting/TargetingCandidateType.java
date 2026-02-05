package com.marketinghub.targeting;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Tipos de candidatos de targeting suportados pela API de Targeting Search do Facebook.
 */
public enum TargetingCandidateType {
    INTEREST,
    BEHAVIOR,
    WORK_POSITION;

    @JsonCreator
    public static TargetingCandidateType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        for (TargetingCandidateType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
