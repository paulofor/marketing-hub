package com.marketinghub.geralanding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GeraLandingResultReceiveRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        @NotBlank String modelResponse,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
