package com.marketinghub.businessprocesscomposition.service.getcomposition;

/** Contrato resumido de um processo que participa de uma composição hierárquica. */
public record BusinessProcessReferenceResponse(
    Long id,
    String processCode,
    String name,
    String purpose,
    String ownerName,
    Integer versionNumber,
    String status,
    String processType) {}
