package com.marketinghub.facebookadsworker.pipeline;

import java.util.Map;

/**
 * Represents the structured output and metrics produced by a pipeline stage.
 */
public record StageResult<O>(
    O output,
    Map<String, Object> metrics
) {
    /**
     * Normalizes nullable metrics maps to an immutable empty map.
     */
    public StageResult {
        if (metrics == null) {
            metrics = Map.of();
        }
    }
}
