package com.marketinghub.experiment.dto;

import java.time.Instant;

public record ExperimentFailureDetailsDto(
        String message,
        String endpoint,
        Integer statusCode,
        Instant occurredAt,
        String source
) {
}
