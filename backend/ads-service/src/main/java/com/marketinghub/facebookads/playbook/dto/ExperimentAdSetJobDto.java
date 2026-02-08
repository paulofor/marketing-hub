package com.marketinghub.facebookads.playbook.dto;

import com.marketinghub.facebookads.playbook.ExperimentAdSetJobStatus;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobType;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;

import java.time.Instant;

/**
 * Exposes job metadata to the UI.
 */
public record ExperimentAdSetJobDto(
        Long id,
        ExperimentAdSetJobType type,
        ExperimentAdSetWorker worker,
        ExperimentAdSetJobStatus status,
        Integer attemptCount,
        String lockedBy,
        Instant lockedAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        Long resourceId,
        Instant createdAt,
        Instant updatedAt
) {
}
