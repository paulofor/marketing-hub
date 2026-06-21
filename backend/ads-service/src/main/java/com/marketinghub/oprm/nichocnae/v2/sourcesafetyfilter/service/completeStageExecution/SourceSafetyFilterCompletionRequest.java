package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato recebido do executor ao concluir a etapa source-safety-filter do NichoCNAE v2. */
public record SourceSafetyFilterCompletionRequest(
        String safetyDecision,
        Integer allowedUrlCount,
        Integer rejectedUrlCount,
        String outputPayload,
        String nextStageCode,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public SourceSafetyFilterCompletionRequest(
            String safetyDecision, Integer allowedUrlCount, Integer rejectedUrlCount, String outputPayload, String nextStageCode) {
        this(safetyDecision, allowedUrlCount, rejectedUrlCount, outputPayload, nextStageCode, List.of());
    }

    /** Mantém compatibilidade com chamadas que ainda não enviam a próxima etapa decidida pelo executor. */
    public SourceSafetyFilterCompletionRequest(
            String safetyDecision, Integer allowedUrlCount, Integer rejectedUrlCount, String outputPayload) {
        this(safetyDecision, allowedUrlCount, rejectedUrlCount, outputPayload, null, List.of());
    }
}
