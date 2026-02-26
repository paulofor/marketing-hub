package com.marketinghub.targeting.mapper;

import com.marketinghub.targeting.dto.TargetingResolutionSummaryDto;
import com.marketinghub.targeting.service.TargetingResolutionJobService.TargetingResolutionSummary;
import org.springframework.stereotype.Component;

@Component
public class TargetingResolutionSummaryMapper {
    public TargetingResolutionSummaryDto toDto(TargetingResolutionSummary summary) {
        if (summary == null) {
            return null;
        }
        return TargetingResolutionSummaryDto.builder()
                .pending(summary.pending())
                .processing(summary.processing())
                .completed(summary.succeeded())
                .failed(summary.failed())
                .lastAttemptAt(summary.lastAttemptAt())
                .lastCompletedAt(summary.lastCompletedAt())
                .lastError(summary.lastError())
                .build();
    }
}
