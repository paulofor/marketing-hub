package com.marketinghub.pipelines.oprm.nichocnae.v3;

/** Representa os estados persistidos de uma execução de etapa do NichoCNAE v3. */
public enum OprmNichoCnaeV3StageExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED
}
