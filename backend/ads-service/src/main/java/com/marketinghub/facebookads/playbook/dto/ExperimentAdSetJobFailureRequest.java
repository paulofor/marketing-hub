package com.marketinghub.facebookads.playbook.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Failure payload sent when a worker cannot complete a job.
 */
public record ExperimentAdSetJobFailureRequest(@NotBlank String errorMessage) {
}
