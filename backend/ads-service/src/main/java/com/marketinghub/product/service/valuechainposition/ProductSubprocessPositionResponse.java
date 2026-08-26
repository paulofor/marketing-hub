package com.marketinghub.product.service.valuechainposition;

import java.util.List;

/** Contrato que explica o subprocesso atual e o próximo objetivo especializado do produto. */
public record ProductSubprocessPositionResponse(
    String trackingStatus,
    Integer subprocessCount,
    String currentActivityName,
    Long currentSubprocessDefinitionId,
    Integer currentSubprocessSequenceNumber,
    String currentSubprocessCode,
    String currentSubprocessName,
    String currentSubprocessObjective,
    Long nextSubprocessDefinitionId,
    Integer nextSubprocessSequenceNumber,
    String nextSubprocessCode,
    String nextSubprocessName,
    String nextSubprocessObjective,
    List<ProductStageMeasurementResponse> measurements) {}
