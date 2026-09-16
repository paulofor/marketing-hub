package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

/**
 * Responsabilidade: identificar a posição de um processo dentro da cadeia preservada pela execução
 * consultada.
 */
public record ProductProcessChainPositionResponse(
    String sequenceLabel, String parentProcessCode, String parentProcessName) {}
