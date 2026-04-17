package com.marketinghub.mds.dto;

import jakarta.validation.constraints.NotBlank;

public record BackendClaimRequestDto(@NotBlank String workerId) {
}
