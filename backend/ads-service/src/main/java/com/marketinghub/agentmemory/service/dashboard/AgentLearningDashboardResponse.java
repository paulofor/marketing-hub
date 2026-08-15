package com.marketinghub.agentmemory.service.dashboard;

import java.util.List;

/** Contrato consolidado do aprendizado comprovável dos agentes. */
public record AgentLearningDashboardResponse(
    long totalMemories,
    long candidateMemories,
    long confirmedMemories,
    long contradictedMemories,
    long retiredMemories,
    long totalRetrievals,
    List<AgentLearningSummaryResponse> agents,
    List<AgentLearningMemoryResponse> memories) {}
