package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa source-safety-filter do NichoCNAE v2. */
public record SourceSafetyFilterCompletionRequest(
        String safetyDecision,
        Integer allowedUrlCount,
        Integer rejectedUrlCount,
        String outputPayload,
        String nextStageCode) {
    /** Mantém compatibilidade com chamadas que ainda não enviam a próxima etapa decidida pelo executor. */
    public SourceSafetyFilterCompletionRequest(
            String safetyDecision, Integer allowedUrlCount, Integer rejectedUrlCount, String outputPayload) {
        this(safetyDecision, allowedUrlCount, rejectedUrlCount, outputPayload, null);
    }
}
