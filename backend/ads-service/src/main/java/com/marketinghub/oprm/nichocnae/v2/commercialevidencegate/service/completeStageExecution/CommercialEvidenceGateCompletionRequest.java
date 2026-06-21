package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato de escrita para registrar o resultado do gate comercial decidido pelo executor externo. */
public record CommercialEvidenceGateCompletionRequest(
        String evidenceLevel,
        Double confidence,
        Boolean automaticMaterializationAllowed,
        Boolean humanReviewRequired,
        Double informationGain,
        String gateDecision,
        String nextStageCode,
        String outputPayload,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public CommercialEvidenceGateCompletionRequest(
            String evidenceLevel,
            Double confidence,
            Boolean automaticMaterializationAllowed,
            Boolean humanReviewRequired,
            Double informationGain,
            String gateDecision,
            String nextStageCode,
            String outputPayload) {
        this(evidenceLevel, confidence, automaticMaterializationAllowed, humanReviewRequired, informationGain, gateDecision, nextStageCode, outputPayload, List.of());
    }
}
