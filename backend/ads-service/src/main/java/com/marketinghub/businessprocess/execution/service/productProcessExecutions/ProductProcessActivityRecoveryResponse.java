package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

/**
 * Responsabilidade: identificar a atividade responsável pela correção e sua disponibilidade, sem
 * atribuir a tarefa de correção à atividade dependente.
 */
public record ProductProcessActivityRecoveryResponse(
    String activityId,
    String activityName,
    Integer sequenceNumber,
    String ownerName,
    String actionLabel,
    boolean actionAvailable,
    String availabilityReason,
    String operationalState,
    boolean objectiveAchieved,
    ProductProcessRecoveryTaskResponse latestTask) {}
