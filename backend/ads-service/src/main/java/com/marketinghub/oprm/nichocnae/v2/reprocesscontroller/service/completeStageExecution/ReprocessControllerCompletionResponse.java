package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.completeStageExecution;

/** Resposta de conclusão persistida da etapa reprocess-controller. */
public record ReprocessControllerCompletionResponse(String stageExecutionId, String status, String executionMode, String rewindToStage, Integer knowledgeVersionTo, String nextStageCode) {}
