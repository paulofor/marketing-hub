package com.marketinghub.hypothesis.dto.internal;

import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record HypothesisFrameworkGenerationJobDto(
        UUID id,
        UUID hypothesisId,
        HypothesisFrameworkSection section,
        String status,
        String stage,
        String customInstructions,
        String errorMessage,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {
}
