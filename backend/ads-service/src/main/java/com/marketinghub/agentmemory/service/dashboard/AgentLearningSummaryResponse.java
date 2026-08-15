package com.marketinghub.agentmemory.service.dashboard;

/** Contrato resumido do aprendizado de um agente. */
public record AgentLearningSummaryResponse(
    String agentKey,
    String agentName,
    long totalMemories,
    long candidateMemories,
    long confirmedMemories,
    long contradictedMemories,
    long retiredMemories,
    long totalRetrievals) {}
