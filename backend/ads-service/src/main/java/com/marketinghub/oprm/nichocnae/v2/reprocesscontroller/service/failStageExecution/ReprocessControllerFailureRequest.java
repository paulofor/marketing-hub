package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato de escrita para registrar falha técnica ou cognitiva da etapa reprocess-controller. */
public record ReprocessControllerFailureRequest(OprmNichoCnaeV2FailureType failureType, String errorMessage, String inputPayload) {}
