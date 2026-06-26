package com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.completeStageExecution;

/** Request de conclusão da etapa quality-gate reportada pelo executor. */
public record QualityGateCompletionRequest(String outputPayload, String nextStageCode) {}
