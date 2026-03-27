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
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {
}
