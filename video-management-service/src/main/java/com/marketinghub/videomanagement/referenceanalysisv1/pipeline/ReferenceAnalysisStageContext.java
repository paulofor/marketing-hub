package com.marketinghub.videomanagement.referenceanalysisv1.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Contexto imutável entregue pelo backend para uma execução de análise. */
public record ReferenceAnalysisStageContext(
        Long executionId,
        Long referenceId,
        String tenantId,
        int attemptNumber,
        String producerExecutionId,
        JsonNode input,
        Instant claimedAt) {
}
