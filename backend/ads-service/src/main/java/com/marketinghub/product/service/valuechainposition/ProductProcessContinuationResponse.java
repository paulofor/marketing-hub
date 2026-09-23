package com.marketinghub.product.service.valuechainposition;

/** Contrato que identifica a continuação publicada após o macroprocesso atual. */
public record ProductProcessContinuationResponse(
    Long processDefinitionId,
    String processCode,
    String processName,
    Integer processVersion,
    Integer sequenceNumber) {}
