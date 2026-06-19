package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato de escrita para registrar falha técnica ou cognitiva da etapa commercial-evidence-gate. */
public record CommercialEvidenceGateFailureRequest(
        OprmNichoCnaeV2FailureType failureType,
        String errorMessage,
        String inputPayload) {}
