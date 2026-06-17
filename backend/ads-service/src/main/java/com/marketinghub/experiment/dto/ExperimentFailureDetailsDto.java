package com.marketinghub.experiment.dto;

import java.time.Instant;

/**
 * Representa os detalhes operacionais da última falha visível no diagnóstico do experimento.
 */
public record ExperimentFailureDetailsDto(
        String message,
        String endpoint,
        Integer statusCode,
        Instant occurredAt,
        String source,
        String jobId
) {
}
