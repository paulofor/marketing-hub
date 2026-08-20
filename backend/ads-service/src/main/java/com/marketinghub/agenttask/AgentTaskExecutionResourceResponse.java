package com.marketinghub.agenttask;

/** Responsabilidade: informar ao executor qual recurso especializado a atividade exige. */
public record AgentTaskExecutionResourceResponse(
    String resourceCode,
    String name,
    String resourceType,
    String executorReference,
    String usageInstructions) {}
