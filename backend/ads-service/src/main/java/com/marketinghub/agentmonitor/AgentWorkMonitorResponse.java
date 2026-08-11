package com.marketinghub.agentmonitor;

import java.time.Instant;

/**
 * Responsabilidade: expor o estado operacional comprovável de um agente na tela de monitoramento.
 */
public record AgentWorkMonitorResponse(
    Long agentId,
    String agentKey,
    String nickname,
    String agentName,
    String workStatus,
    String currentWork,
    String progressDetail,
    String difficulty,
    boolean externalDecisionRequired,
    String externalDecision,
    String sourceReference,
    Instant lastActivityAt) {}
