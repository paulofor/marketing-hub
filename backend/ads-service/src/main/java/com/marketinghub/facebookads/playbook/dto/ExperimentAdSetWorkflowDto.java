package com.marketinghub.facebookads.playbook.dto;

import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflowStatus;
import com.marketinghub.experiment.ExperimentStatus;

import java.time.Instant;
import java.util.List;

/**
 * Detailed representation of the workflow for the frontend.
 */
public record ExperimentAdSetWorkflowDto(
        Long workflowId,
        Long experimentId,
        ExperimentStatus experimentStatus,
        ExperimentAdSetWorkflowStatus status,
        String seedKeyword,
        String seedLocale,
        String seedInterestId,
        String seedInterestName,
        Long seedAudienceLower,
        Long seedAudienceUpper,
        String aiNotes,
        String lastError,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ExperimentAdSetJobDto> jobs,
        List<ExperimentAdSetSpecDto> specs
) {
}
