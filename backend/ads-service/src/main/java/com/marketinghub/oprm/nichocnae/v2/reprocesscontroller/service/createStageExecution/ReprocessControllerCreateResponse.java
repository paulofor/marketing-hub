package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.createStageExecution;

/** Resposta de criação de pendência da etapa reprocess-controller. */
public record ReprocessControllerCreateResponse(String stageExecutionId, String status, String stageCode) {}
