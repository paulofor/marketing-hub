package com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.failStageExecution;

/** Request de falha da etapa quality-gate reportada pelo executor. */
public record QualityGateFailureRequest(String errorMessage) {}
