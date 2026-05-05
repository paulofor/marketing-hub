package com.marketinghub.geralanding;

import java.time.Instant;

public record GeraLandingExecutionSummaryResponse(String idJob, String status, Instant executionRequestedAt) {
}
