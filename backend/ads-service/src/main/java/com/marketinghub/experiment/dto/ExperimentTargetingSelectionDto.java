package com.marketinghub.experiment.dto;

import com.marketinghub.targeting.TargetingCandidateType;
import lombok.Builder;

@Builder
public record ExperimentTargetingSelectionDto(
        Long id,
        Long experimentId,
        TargetingCandidateType candidateType,
        String term
) {
}
