package com.marketinghub.facebookads.playbook.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Failure payload sent when a worker cannot complete a job.
 */
public record ExperimentAdSetJobFailureRequest(
        @NotBlank String errorMessage,
        List<ExperimentAdSetJobApiLogRequest> apiCalls
) {
}
