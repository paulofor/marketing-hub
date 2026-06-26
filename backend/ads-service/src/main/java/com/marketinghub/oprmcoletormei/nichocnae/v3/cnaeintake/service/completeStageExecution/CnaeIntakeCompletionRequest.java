package com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service.completeStageExecution;

/** Request de conclusão da etapa cnae-intake reportada pelo executor. */
public record CnaeIntakeCompletionRequest(String outputPayload, String nextStageCode) {}
