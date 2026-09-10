package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

/**
 * Responsabilidade: expor o comando canônico que trata o bloqueio de outra atividade do processo.
 */
public record ProductProcessActivityRecoveryResponse(
    String activityId,
    String activityName,
    String ownerName,
    String actionLabel,
    boolean actionAvailable,
    String availabilityReason) {}
