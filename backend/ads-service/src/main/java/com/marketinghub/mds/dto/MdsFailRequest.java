package com.marketinghub.mds.dto;

import jakarta.validation.constraints.NotBlank;

public record MdsFailRequest(
        @NotBlank String reason,
        String stageName,
        String message
) {
}
