package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

/**
 * Responsabilidade: explicar um pré-requisito persistido para executar uma atividade do produto.
 */
public record ProductProcessActivityRequirementResponse(
    String code, String title, boolean satisfied, String detail, String recommendation) {}
