package com.marketinghub.facebookads.playbook.dto;

import com.marketinghub.facebookads.playbook.ExperimentAdSetJobStatus;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobType;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;

import java.time.Instant;

public record ExperimentFacebookApiLogDto(
        Long id,
        Long jobId,
        ExperimentAdSetJobType jobType,
        ExperimentAdSetWorker jobWorker,
        ExperimentAdSetJobStatus jobStatus,
        Long workflowId,
        Long resourceId,
        String provider,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        String errorMessage,
        Instant requestedAt,
        Instant respondedAt,
        Long durationMs,
        String requestPayload,
        String responsePayload,
        Instant createdAt
) {
}
