package com.marketinghub.facebookads.playbook.dto;

import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body used by workers to claim jobs.
 */
public record ExperimentAdSetJobClaimRequest(
        @NotNull ExperimentAdSetWorker worker,
        @Min(1) @Max(20) int limit,
        @NotBlank String workerId
) {
}
