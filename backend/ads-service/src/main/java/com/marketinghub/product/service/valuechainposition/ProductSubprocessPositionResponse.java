package com.marketinghub.product.service.valuechainposition;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow.SalesFlowResponse;
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
    List<ProductStageMeasurementResponse> measurements,
    SalesFlowResponse salesFlow) {
  /** Mantém compatibilidade com processos sem contrato de fluxo comercial. */
  public ProductSubprocessPositionResponse(
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
      List<ProductStageMeasurementResponse> measurements) {
    this(
        trackingStatus,
        subprocessCount,
        currentActivityName,
        currentSubprocessDefinitionId,
        currentSubprocessSequenceNumber,
        currentSubprocessCode,
        currentSubprocessName,
        currentSubprocessObjective,
        nextSubprocessDefinitionId,
        nextSubprocessSequenceNumber,
        nextSubprocessCode,
        nextSubprocessName,
        nextSubprocessObjective,
        measurements,
        null);
  }
}
