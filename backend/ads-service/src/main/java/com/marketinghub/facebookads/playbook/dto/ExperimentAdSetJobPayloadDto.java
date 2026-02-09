package com.marketinghub.facebookads.playbook.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobType;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;

import java.time.Instant;

/**
 * Job payload delivered to background workers.
 */
public record ExperimentAdSetJobPayloadDto(
        Long id,
        ExperimentAdSetJobType type,
        ExperimentAdSetWorker worker,
        Long workflowId,
        Long resourceId,
        JsonNode payload,
        Instant createdAt
) {
}
