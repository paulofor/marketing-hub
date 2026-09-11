package com.marketinghub.product.service.agentvalidation;

/** Responsabilidade: transportar somente os dados persistidos necessários ao gate de retrabalho. */
public record PdeValidationTaskSnapshot(
    Long id,
    Long processDefinitionId,
    String processActivityId,
    String status,
    String blockerCategory,
    String blockerAction,
    String resultJson,
    String executionError) {}
