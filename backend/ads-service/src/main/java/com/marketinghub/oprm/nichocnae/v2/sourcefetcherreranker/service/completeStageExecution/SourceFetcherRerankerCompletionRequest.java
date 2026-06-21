package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato recebido do executor ao concluir a etapa source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerCompletionRequest(
        String sourceFetchDecision,
        Integer fetchedSnapshotCount,
        Integer selectedSourceCount,
        Integer rejectedSourceCount,
        String outputPayload,
        String nextStageCode,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public SourceFetcherRerankerCompletionRequest(
            String sourceFetchDecision,
            Integer fetchedSnapshotCount,
            Integer selectedSourceCount,
            Integer rejectedSourceCount,
            String outputPayload,
            String nextStageCode) {
        this(sourceFetchDecision, fetchedSnapshotCount, selectedSourceCount, rejectedSourceCount, outputPayload, nextStageCode, List.of());
    }
}
