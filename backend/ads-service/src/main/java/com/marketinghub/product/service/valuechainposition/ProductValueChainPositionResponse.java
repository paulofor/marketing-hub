package com.marketinghub.product.service.valuechainposition;

/** Contrato que localiza um produto dentro da cadeia de valor publicada. */
public record ProductValueChainPositionResponse(
    Long productId,
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
    Integer processCount,
    ProductSubprocessPositionResponse subprocessPosition) {}
