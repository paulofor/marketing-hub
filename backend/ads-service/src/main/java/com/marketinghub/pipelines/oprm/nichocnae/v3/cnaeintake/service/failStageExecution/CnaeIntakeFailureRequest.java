package com.marketinghub.pipelines.oprm.nichocnae.v3.cnaeintake.service.failStageExecution;

/** Request de falha da etapa cnae-intake reportada pelo executor. */
public record CnaeIntakeFailureRequest(String errorMessage) {}
