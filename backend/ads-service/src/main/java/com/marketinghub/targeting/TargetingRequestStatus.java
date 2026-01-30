package com.marketinghub.targeting;

/**
 * Status do ciclo de vida de uma solicitação de targeting feita pelo cliente.
 */
public enum TargetingRequestStatus {
    PENDING_AI,
    COMPLETED,
    FAILED
}
