package com.marketinghub.businessprocess.execution.service.backendactivity;

/** Responsabilidade: devolver o resultado auditável de uma atividade executada pelo backend. */
public record BackendProductProcessActivityExecutionResult(
    String sourceReference, String operationalState, boolean objectiveAchieved, String message) {}
