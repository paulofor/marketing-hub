package com.marketinghub.agentdetail.service.getDetail;

/** Responsabilidade: expor um recurso executável ativo pertencente ao agente detalhado. */
public record AgentDetailResourceResponse(
    Long id,
    String resourceCode,
    String name,
    String description,
    String resourceType,
    String executorReference,
    String usageInstructions) {}
