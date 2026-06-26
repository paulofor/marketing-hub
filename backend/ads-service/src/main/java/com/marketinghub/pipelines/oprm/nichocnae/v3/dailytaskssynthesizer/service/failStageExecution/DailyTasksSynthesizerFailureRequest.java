package com.marketinghub.pipelines.oprm.nichocnae.v3.dailytaskssynthesizer.service.failStageExecution;

/** Request de falha da etapa daily-tasks-synthesizer reportada pelo executor. */
public record DailyTasksSynthesizerFailureRequest(String errorMessage) {}
