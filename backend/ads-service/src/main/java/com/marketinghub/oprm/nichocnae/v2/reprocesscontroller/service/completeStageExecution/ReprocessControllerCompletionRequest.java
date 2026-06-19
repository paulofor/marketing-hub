package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.completeStageExecution;

/** Contrato de escrita para registrar plano de retry ou reprocessamento decidido pelo executor externo. */
public record ReprocessControllerCompletionRequest(String executionMode, String rewindToStage, Integer knowledgeVersionTo, String nextStageCode, String outputPayload) {}
