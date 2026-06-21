package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato de escrita para registrar a materialização decidida e executada pelo executor externo. */
public record EnrichedNicheMaterializerCompletionRequest(
        String materializationDecision,
        String validationLevel,
        Double confidence,
        Long materializedNicheId,
        String nextStageCode,
        String outputPayload,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public EnrichedNicheMaterializerCompletionRequest(
            String materializationDecision,
            String validationLevel,
            Double confidence,
            Long materializedNicheId,
            String nextStageCode,
            String outputPayload) {
        this(materializationDecision, validationLevel, confidence, materializedNicheId, nextStageCode, outputPayload, List.of());
    }
}
