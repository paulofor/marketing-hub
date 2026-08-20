package com.marketinghub.businessprocesschain.service.getChain;

/** Resposta de um processo versionado na posição em que cria valor dentro da cadeia. */
public record BusinessProcessChainProcessResponse(
    Integer sequenceNumber,
    String valueContribution,
    Long processDefinitionId,
    String processCode,
    String name,
    String purpose,
    String ownerName,
    String triggerDescription,
    String outcomeDescription,
    Integer versionNumber,
    String status) {}
