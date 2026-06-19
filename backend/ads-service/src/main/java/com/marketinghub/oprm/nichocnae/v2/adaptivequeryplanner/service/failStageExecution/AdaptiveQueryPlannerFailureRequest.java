package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato recebido do executor para registrar falha da etapa adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerFailureRequest(
        OprmNichoCnaeV2FailureType failureType, String errorMessage, String inputPayload) {}
