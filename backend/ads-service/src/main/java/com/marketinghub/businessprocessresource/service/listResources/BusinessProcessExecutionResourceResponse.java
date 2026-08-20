package com.marketinghub.businessprocessresource.service.listResources;

/** Responsabilidade: expor um recurso especializado selecionável por uma atividade de processo. */
public record BusinessProcessExecutionResourceResponse(
    Long id,
    String resourceCode,
    String name,
    String description,
    String resourceType,
    String responsibleAgentKey,
    String executorReference,
    String usageInstructions) {}
