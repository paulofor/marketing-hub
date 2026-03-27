package com.marketinghub.worker.hypothesisframework;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HypothesisFrameworkJobDto(
        UUID id,
        UUID hypothesisId,
        String section,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {

    /**
     * Backward-compatible accessor kept to avoid breaking callers still using the previous field name.
     * The backend payload currently exposes {@code hypothesisId}, which is what this alias returns.
     */
    public UUID experimentId() {
        return hypothesisId;
    }
}
