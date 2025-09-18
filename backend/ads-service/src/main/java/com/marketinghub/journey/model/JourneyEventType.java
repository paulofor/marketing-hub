package com.marketinghub.journey.model;

/**
 * Canonical event types emitted during journey execution.
 */
public enum JourneyEventType {
    STIMULUS_DISPATCHED("journey.stimulus.dispatched"),
    STIMULUS_FAILED("journey.stimulus.failed"),
    STIMULUS_SKIPPED("journey.stimulus.skipped"),
    STIMULUS_FREQUENCY_CAPPED("journey.stimulus.frequency_capped"),
    JOURNEY_COMPLETED("journey.completed");

    private final String code;

    JourneyEventType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
