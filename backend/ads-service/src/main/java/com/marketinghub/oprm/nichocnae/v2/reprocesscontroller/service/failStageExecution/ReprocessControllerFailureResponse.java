package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.failStageExecution;

/** Resposta de falha da etapa reprocess-controller, incluindo retry técnico quando aplicável. */
public record ReprocessControllerFailureResponse(String stageExecutionId, String status, String retryStageExecutionId, Integer attemptNumber, Integer technicalRetryNumber) {}
