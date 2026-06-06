package com.marketinghub.facebookadsworker.pipeline;

import java.util.Map;

/**
 * Carries the input and execution metadata needed by a pipeline stage.
 */
public record StageContext<I>(
    String stageName,
    String executionId,
    I input,
    Map<String, Object> config
) {
    /**
     * Normalizes nullable configuration maps to an immutable empty map.
     */
    public StageContext {
        if (config == null) {
            config = Map.of();
        }
    }
}
