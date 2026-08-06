package com.marketinghub.codextelemetry;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar a telemetria Codex pela identidade auditável da execução. */
public interface CodexAgentExecutionTelemetryRepository
    extends JpaRepository<CodexAgentExecutionTelemetry, Long> {
  /** Localiza a telemetria de um job específico de agente. */
  Optional<CodexAgentExecutionTelemetry> findByAgentTypeAndExecutionId(
      String agentType, Long executionId);
}
