package com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.completeStageExecution;

/** Request de conclusão da etapa daily-tasks-synthesizer reportada pelo executor. */
public record DailyTasksSynthesizerCompletionRequest(String outputPayload, String nextStageCode) {}
