package com.marketinghub.facebookads.playbook.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Result payload posted by a worker after processing a job.
 */
public record ExperimentAdSetJobResultRequest(@NotNull JsonNode result) {
}
