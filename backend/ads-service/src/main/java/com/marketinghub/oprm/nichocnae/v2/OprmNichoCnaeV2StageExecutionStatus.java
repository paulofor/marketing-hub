package com.marketinghub.oprm.nichocnae.v2;

/** Define o estado persistido de uma execução imutável de etapa do pipeline NichoCNAE v2. */
public enum OprmNichoCnaeV2StageExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    TECHNICAL_RETRY_SCHEDULED,
    FAILED
}
