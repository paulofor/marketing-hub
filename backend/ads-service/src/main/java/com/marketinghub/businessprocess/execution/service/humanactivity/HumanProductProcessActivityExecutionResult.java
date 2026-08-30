package com.marketinghub.businessprocess.execution.service.humanactivity;

/** Responsabilidade: devolver o estado auditável alcançado por uma decisão humana. */
public record HumanProductProcessActivityExecutionResult(
    String sourceReference, String operationalState, boolean objectiveAchieved, String message) {}
