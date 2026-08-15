package com.marketinghub.repository.jpa.codextelemetry;

import com.marketinghub.codextelemetry.CodexAgentExecutionTelemetry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: acessar a telemetria Codex pela identidade auditável da execução. */
public interface CodexAgentExecutionTelemetryRepository
    extends JpaRepository<CodexAgentExecutionTelemetry, Long> {
  /** Localiza a telemetria de um job específico de agente. */
  Optional<CodexAgentExecutionTelemetry> findByAgentTypeAndExecutionId(
      String agentType, Long executionId);

  /** Localiza o sinal mais recente do executor para o painel operacional do agente. */
  Optional<CodexAgentExecutionTelemetry> findTopByAgentTypeOrderByUpdatedAtDescIdDesc(
      String agentType);

  /** Soma os tokens das execuções iniciadas dentro da janela diária solicitada. */
  @Query(
      "SELECT telemetry.agentType, "
          + "COALESCE(SUM(COALESCE(telemetry.inputTokens, 0) + COALESCE(telemetry.outputTokens, 0)), 0) "
          + "FROM CodexAgentExecutionTelemetry telemetry "
          + "WHERE telemetry.startedAt >= :start AND telemetry.startedAt < :end "
          + "GROUP BY telemetry.agentType")
  List<Object[]> sumTokensByAgentTypeBetween(
      @Param("start") Instant start, @Param("end") Instant end);
}
