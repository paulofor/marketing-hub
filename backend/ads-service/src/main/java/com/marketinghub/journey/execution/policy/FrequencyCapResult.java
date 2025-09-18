package com.marketinghub.journey.execution.policy;

import java.time.Instant;
import java.util.Map;

/**
 * Result of evaluating the frequency cap policy for a stimulus.
 */
public record FrequencyCapResult(boolean blocked, Instant nextAttemptAt, Map<String, Object> metadata) {
    public static FrequencyCapResult allow() {
        return new FrequencyCapResult(false, null, Map.of());
    }

    public static FrequencyCapResult block(Instant nextAttemptAt, Map<String, Object> metadata) {
        return new FrequencyCapResult(true, nextAttemptAt, metadata == null ? Map.of() : metadata);
    }
}
