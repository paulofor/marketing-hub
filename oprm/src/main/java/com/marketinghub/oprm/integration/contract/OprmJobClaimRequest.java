package com.marketinghub.oprm.integration.contract;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OprmJobClaimRequest(
        @NotBlank String workerId,
        @NotBlank String workerVersion,
        @NotBlank String contractVersion,
        @Min(1) int maxJobs,
        @Min(30) int leaseSeconds
) {
}
