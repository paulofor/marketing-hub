package com.marketinghub.product.service.valuechainposition;

import java.util.List;

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
    ProductProcessContinuationResponse nextProcess,
    List<ProductStageMeasurementResponse> processMeasurements,
    ProductSubprocessPositionResponse subprocessPosition,
    ProductLearningCycleNavigationResponse learningCycleNavigation) {
  /** Preserva os consumidores internos que ainda não fornecem uma navegação de ciclo. */
  public ProductValueChainPositionResponse(
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
      ProductProcessContinuationResponse nextProcess,
      List<ProductStageMeasurementResponse> processMeasurements,
      ProductSubprocessPositionResponse subprocessPosition) {
    this(
        productId,
        commercialStatus,
        resolutionStatus,
        resolutionMessage,
        chainDefinitionId,
        chainName,
        chainVersion,
        processDefinitionId,
        processCode,
        processName,
        processVersion,
        sequenceNumber,
        processCount,
        nextProcess,
        processMeasurements,
        subprocessPosition,
        null);
  }
}
