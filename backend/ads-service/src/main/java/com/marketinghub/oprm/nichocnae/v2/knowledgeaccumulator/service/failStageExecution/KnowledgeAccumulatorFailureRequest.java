package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato recebido do executor para registrar falha da etapa knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorFailureRequest(
        OprmNichoCnaeV2FailureType failureType, String errorMessage, String inputPayload) {}
