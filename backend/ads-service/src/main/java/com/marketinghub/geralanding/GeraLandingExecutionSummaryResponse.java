package com.marketinghub.geralanding;

import java.math.BigDecimal;
import java.time.Instant;

/** @deprecated usar versões por etapa em geralanding.<etapa>.service. */
@Deprecated
public record GeraLandingExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
