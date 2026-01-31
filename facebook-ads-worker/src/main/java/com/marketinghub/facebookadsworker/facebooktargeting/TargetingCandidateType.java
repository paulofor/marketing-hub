package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipos de candidatos suportados pelo resolver/Graph API.
 */
public enum TargetingCandidateType {
    INTEREST("interest"),
    BEHAVIOR("behavior"),
    WORK_POSITION("work_position");

    private final String jsonValue;

    TargetingCandidateType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static TargetingCandidateType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        for (TargetingCandidateType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
