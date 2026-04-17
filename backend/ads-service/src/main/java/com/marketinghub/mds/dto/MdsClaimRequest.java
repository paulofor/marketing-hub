package com.marketinghub.mds.dto;

import jakarta.validation.constraints.NotBlank;

public record MdsClaimRequest(@NotBlank String workerId) {
}
