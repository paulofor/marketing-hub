package com.marketinghub.geralanding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeraLandingStageStartRequest(
        @NotBlank String stageCode,
        @NotNull @Valid Prompt prompt) {

    public record Prompt(
            String templateId,
            @NotBlank String content) {
    }
}
