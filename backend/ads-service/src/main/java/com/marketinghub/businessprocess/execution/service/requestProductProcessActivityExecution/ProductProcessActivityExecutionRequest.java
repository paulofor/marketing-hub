package com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution;

/** Responsabilidade: transportar a decisão explícita usada para executar uma atividade humana. */
public record ProductProcessActivityExecutionRequest(
    String decision,
    String operatorName,
    String justification,
    String evidenceReference,
    String confirmationToken) {}
