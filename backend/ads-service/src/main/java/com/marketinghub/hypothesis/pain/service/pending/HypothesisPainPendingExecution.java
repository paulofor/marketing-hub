package com.marketinghub.hypothesis.pain.service.pending;

import java.time.Instant;

/** Execução pendente de etapa do pipeline de hipótese entregue ao Worker AI. */
public record HypothesisPainPendingExecution(
        Long marketNicheId,
        String jobid,
        String stageCode,
        String status,
        Instant executionRequestedAt,
        Instant processingStartedAt,
        HypothesisPainPendingNiche niche,
        HypothesisPainPendingEnrichmentProfile enrichmentProfile,
        String painModelResponse,
        String resultModelResponse,
        String mechanismModelResponse,
        String proofModelResponse
) {
}
