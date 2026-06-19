package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.completeStageExecution;

/** Contrato de escrita para registrar o resultado do gate comercial decidido pelo executor externo. */
public record CommercialEvidenceGateCompletionRequest(
        String evidenceLevel,
        Double confidence,
        Boolean automaticMaterializationAllowed,
        Boolean humanReviewRequired,
        Double informationGain,
        String gateDecision,
        String nextStageCode,
        String outputPayload) {}
