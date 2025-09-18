package com.marketinghub.journey.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Four classic phases of the AIDA framework used to structure journeys.
 */
public enum JourneyPhase {
    ATTENTION("A"),
    INTEREST("I"),
    DESIRE("D"),
    ACTION("A");

    private final String code;

    JourneyPhase(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @JsonCreator
    public static JourneyPhase fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "A", "ATTENTION", "ATENCAO" -> ATTENTION;
            case "I", "INTEREST", "INTERESSE" -> INTEREST;
            case "D", "DESIRE", "DESEJO" -> DESIRE;
            case "ACTION", "ACAO", "A2", "ACAO_FINAL" -> ACTION;
            default -> {
                for (JourneyPhase phase : JourneyPhase.values()) {
                    if (phase.name().equalsIgnoreCase(value)) {
                        yield phase;
                    }
                }
                throw new IllegalArgumentException("Unknown journey phase: " + value);
            }
        };
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
