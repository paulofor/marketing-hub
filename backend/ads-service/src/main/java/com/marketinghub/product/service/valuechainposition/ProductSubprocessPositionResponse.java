package com.marketinghub.product.service.valuechainposition;

/** Contrato que explica o subprocesso atual e o próximo objetivo especializado do produto. */
public record ProductSubprocessPositionResponse(
    String trackingStatus,
    Integer subprocessCount,
    String currentActivityName,
    Long currentSubprocessDefinitionId,
    String currentSubprocessCode,
    String currentSubprocessName,
    String currentSubprocessObjective,
    Long nextSubprocessDefinitionId,
    String nextSubprocessCode,
    String nextSubprocessName,
    String nextSubprocessObjective) {}
