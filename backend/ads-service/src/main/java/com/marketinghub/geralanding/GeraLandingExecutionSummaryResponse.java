package com.marketinghub.geralanding;

import java.math.BigDecimal;
import java.time.Instant;

public record GeraLandingExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
