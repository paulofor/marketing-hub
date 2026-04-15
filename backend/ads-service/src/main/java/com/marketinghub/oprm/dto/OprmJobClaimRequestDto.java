package com.marketinghub.oprm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OprmJobClaimRequestDto(
        @NotBlank String workerId,
        @NotBlank String workerVersion,
        @NotBlank String contractVersion,
        @Min(1) int maxJobs,
        @Min(30) int leaseSeconds
) {
}
