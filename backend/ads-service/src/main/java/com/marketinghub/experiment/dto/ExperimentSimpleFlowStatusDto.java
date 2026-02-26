package com.marketinghub.experiment.dto;

import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.dto.TargetingResolutionSummaryDto;
import lombok.Builder;

@Builder
public record ExperimentSimpleFlowStatusDto(
        TargetingRequestDto request,
        TargetingResolutionSummaryDto resolution
) {
}
