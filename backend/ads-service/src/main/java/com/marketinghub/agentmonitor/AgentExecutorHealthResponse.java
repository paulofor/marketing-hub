package com.marketinghub.agentmonitor;

import java.time.Instant;

/** Responsabilidade: expor a prontidão operacional consolidada de um executor. */
public record AgentExecutorHealthResponse(
    String status,
    Integer expectedVersion,
    Integer deployedVersion,
    boolean versionCurrent,
    boolean backendAccessible,
    boolean codexAuthenticated,
    String buildReference,
    String detail,
    Instant checkedAt) {
  /** Representa um agente que ainda não enviou nenhuma prova operacional. */
  public static AgentExecutorHealthResponse unknown(Integer expectedVersion) {
    return new AgentExecutorHealthResponse(
        "UNKNOWN",
        expectedVersion,
        null,
        false,
        false,
        false,
        null,
        "Executor ainda não enviou uma verificação completa.",
        null);
  }
}
