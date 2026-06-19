package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato recebido do executor ao reportar falha da etapa candidate-generator do NichoCNAE v2. */
public record CandidateGeneratorFailureRequest(
        OprmNichoCnaeV2FailureType failureType,
        String reasonCode,
        String errorMessage,
        String inputPayload) {}
