package com.marketinghub.product.service.valuechainposition.summary;

/**
 * Responsabilidade: transportar a posição atual sem consultar o histórico de tarefas do produto.
 */
public record ProductValueChainSummaryResponse(
    Long productId,
    String productName,
    String productInternalName,
    String commercialStatus,
    String resolutionStatus,
    String resolutionMessage,
    Long chainDefinitionId,
    String chainName,
    Integer chainVersion,
    Long processDefinitionId,
    String processCode,
    String processName,
    Integer processVersion,
    Integer sequenceNumber,
    Integer processCount) {}
