package com.marketinghub.businessprocess.independent.service.catalog;

import java.util.List;

/** Contrato de um processo publicado que pode nascer sem produto. */
public record IndependentBusinessProcessCatalogResponse(
    Long processDefinitionId,
    String processCode,
    String name,
    String purpose,
    String ownerName,
    String triggerDescription,
    String outcomeDescription,
    Integer versionNumber,
    boolean executionAvailable,
    String executionAvailabilityReason,
    List<IndependentBusinessProcessInputFieldResponse> inputFields) {}
