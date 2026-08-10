package com.marketinghub.geralanding.agent.v1;

import java.time.Instant;
import java.util.Map;

/** Contrato da execução congelada entregue ao Agente Gerador de Landing. */
public record LandingAgentPendingResponse(
    String executionId, Long experimentId, Instant requestedAt, Map<String, Object> context) {}
