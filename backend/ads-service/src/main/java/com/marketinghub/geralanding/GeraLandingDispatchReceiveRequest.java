package com.marketinghub.geralanding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeraLandingDispatchReceiveRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        @NotBlank String openAiJobId) {
}
