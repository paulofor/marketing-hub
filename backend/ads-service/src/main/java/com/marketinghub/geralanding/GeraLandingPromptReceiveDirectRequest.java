package com.marketinghub.geralanding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeraLandingPromptReceiveDirectRequest(
        @NotBlank String idJob,
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        @NotBlank String prompt
) {
}
