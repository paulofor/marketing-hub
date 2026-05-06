package com.marketinghub.worker.geralanding;

import java.time.Instant;
import java.util.UUID;

public record GeraLandingJobDto(
        UUID id,
        Long experimentId,
        String section,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {
}
