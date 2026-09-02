package com.marketinghub.businessprocess.independent.service.executions;

import java.time.Instant;

/**
 * Responsabilidade: transportar a identidade leve de uma execução independente sem hidratar os
 * contratos extensos do processo.
 */
public record IndependentBusinessProcessExecutionListSnapshot(
    Long id,
    String requestKey,
    Long processDefinitionId,
    String processCode,
    String processName,
    Integer processVersionNumber,
    String sourceReference,
    String displayName,
    String requestedByName,
    String inputJson,
    Instant createdAt) {}
