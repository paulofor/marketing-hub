package com.marketinghub.hypothesis.pain.service.pending;

import java.time.Instant;

/** Execução pendente da etapa Dor entregue ao Worker AI. */
public record HypothesisPainPendingExecution(
        Long marketNicheId,
        String jobid,
        String stageCode,
        Instant executionRequestedAt,
        HypothesisPainPendingNiche niche,
        String painModelResponse,
        String resultModelResponse
) {
}
