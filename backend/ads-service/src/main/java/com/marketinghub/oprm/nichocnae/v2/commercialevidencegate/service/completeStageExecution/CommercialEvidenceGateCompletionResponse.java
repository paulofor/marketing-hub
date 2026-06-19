package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.completeStageExecution;

/** Resposta de conclusão persistida da etapa commercial-evidence-gate. */
public record CommercialEvidenceGateCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        String evidenceLevel,
        Double confidence,
        Boolean automaticMaterializationAllowed,
        Boolean humanReviewRequired,
        Double informationGain,
        String gateDecision) {}
