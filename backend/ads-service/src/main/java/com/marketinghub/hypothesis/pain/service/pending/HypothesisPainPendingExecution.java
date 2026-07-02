package com.marketinghub.hypothesis.pain.service.pending;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> promptTemplate,
        List<HypothesisPainPendingExistingHypothesis> existingHypotheses,
        String painModelResponse,
        String resultModelResponse,
        String mechanismModelResponse,
        String proofModelResponse
) {
}
