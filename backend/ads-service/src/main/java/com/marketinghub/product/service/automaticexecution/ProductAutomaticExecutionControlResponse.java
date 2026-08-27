package com.marketinghub.product.service.automaticexecution;

import java.time.Instant;

/** Responsabilidade: expor a verdade persistida do controle PLAY/STOP de um produto. */
public record ProductAutomaticExecutionControlResponse(
    Long productId,
    boolean automaticExecutionEnabled,
    String automaticExecutionStatus,
    Instant changedAt,
    String changedBy) {}
