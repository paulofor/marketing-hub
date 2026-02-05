package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TargetingResolutionSummaryDto {
    int pending;
    int processing;
    int completed;
    int failed;

    @JsonProperty("last_attempt_at")
    Instant lastAttemptAt;

    @JsonProperty("last_completed_at")
    Instant lastCompletedAt;

    @JsonProperty("last_error")
    String lastError;
}
