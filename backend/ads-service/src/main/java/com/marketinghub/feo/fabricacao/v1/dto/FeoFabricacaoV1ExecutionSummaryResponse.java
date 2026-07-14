package com.marketinghub.feo.fabricacao.v1.dto;

import java.time.Instant;

/** Responsabilidade: resumir uma execução FEO para consulta operacional do experimento. */
public record FeoFabricacaoV1ExecutionSummaryResponse(
        Long executionId,
        String jobId,
        String stageCode,
        String status,
        String blockReason,
        String errorMessage,
        Instant createdAt,
        Instant finishedAt) {
}
