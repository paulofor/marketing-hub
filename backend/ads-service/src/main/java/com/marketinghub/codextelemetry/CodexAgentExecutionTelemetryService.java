package com.marketinghub.codextelemetry;

import com.marketinghub.repository.jpa.codextelemetry.CodexAgentExecutionTelemetryRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: consolidar heartbeats e conclusão das execuções Codex dos agentes. */
@Service
public class CodexAgentExecutionTelemetryService {
  private static final Duration STALE_AFTER = Duration.ofMinutes(2);
  private final CodexAgentExecutionTelemetryRepository repository;

  /** Configura a persistência da telemetria. */
  public CodexAgentExecutionTelemetryService(CodexAgentExecutionTelemetryRepository repository) {
    this.repository = repository;
  }

  /** Inicia ou atualiza a execução sem reduzir contadores já recebidos. */
  @Transactional
  public Response heartbeat(String agentType, Long executionId, HeartbeatRequest request) {
    Instant now = Instant.now();
    CodexAgentExecutionTelemetry value =
        repository
            .findByAgentTypeAndExecutionId(agentType, executionId)
            .orElseGet(() -> fresh(agentType, executionId, now));
    value.setStatus("RUNNING");
    value.setProcessId(request.processId());
    value.setProcessAlive(request.processAlive());
    value.setEventCount(max(value.getEventCount(), request.eventCount()));
    value.setOutputBytes(max(value.getOutputBytes(), request.outputBytes()));
    value.setInputTokens(nullableMax(value.getInputTokens(), request.inputTokens()));
    value.setOutputTokens(nullableMax(value.getOutputTokens(), request.outputTokens()));
    value.setLastEventType(request.lastEventType());
    value.setLastActivityAt(now);
    return response(repository.save(value), now);
  }

  /** Marca a execução como concluída ou falha preservando a última medição. */
  @Transactional
  public Response finish(String agentType, Long executionId, FinishRequest request) {
    CodexAgentExecutionTelemetry value =
        repository
            .findByAgentTypeAndExecutionId(agentType, executionId)
            .orElseGet(() -> fresh(agentType, executionId, Instant.now()));
    value.setStatus(Boolean.TRUE.equals(request.success()) ? "COMPLETED" : "FAILED");
    value.setProcessAlive(false);
    value.setEventCount(max(value.getEventCount(), request.eventCount()));
    value.setOutputBytes(max(value.getOutputBytes(), request.outputBytes()));
    value.setInputTokens(nullableMax(value.getInputTokens(), request.inputTokens()));
    value.setOutputTokens(nullableMax(value.getOutputTokens(), request.outputTokens()));
    value.setLastEventType(Boolean.TRUE.equals(request.success()) ? "COMPLETED" : "FAILED");
    value.setLastActivityAt(Instant.now());
    value.setFinishedAt(Instant.now());
    return response(repository.save(value), Instant.now());
  }

  /** Retorna o diagnóstico atual da execução. */
  @Transactional(readOnly = true)
  public Response get(String agentType, Long executionId) {
    return repository
        .findByAgentTypeAndExecutionId(agentType, executionId)
        .map(value -> response(value, Instant.now()))
        .orElse(null);
  }

  /** Cria a medição inicial com contadores neutros. */
  private CodexAgentExecutionTelemetry fresh(String agentType, Long executionId, Instant now) {
    CodexAgentExecutionTelemetry value = new CodexAgentExecutionTelemetry();
    value.setAgentType(agentType);
    value.setExecutionId(executionId);
    value.setStatus("RUNNING");
    value.setProcessAlive(false);
    value.setEventCount(0L);
    value.setOutputBytes(0L);
    value.setStartedAt(now);
    value.setLastActivityAt(now);
    return value;
  }

  /** Converte a entidade e calcula se o heartbeat está atrasado. */
  private Response response(CodexAgentExecutionTelemetry value, Instant now) {
    boolean stale =
        "RUNNING".equals(value.getStatus())
            && value.getLastActivityAt() != null
            && value.getLastActivityAt().plus(STALE_AFTER).isBefore(now);
    return new Response(
        value.getAgentType(),
        value.getExecutionId(),
        value.getStatus(),
        value.getProcessId(),
        value.getProcessAlive(),
        value.getEventCount(),
        value.getOutputBytes(),
        value.getInputTokens(),
        value.getOutputTokens(),
        value.getLastEventType(),
        value.getLastActivityAt(),
        value.getStartedAt(),
        value.getFinishedAt(),
        stale);
  }

  /** Mantém contadores monotônicos. */
  private long max(Long current, Long received) {
    return Math.max(current == null ? 0 : current, received == null ? 0 : received);
  }

  /** Mantém tokens nulos quando o Codex ainda não os informou. */
  private Long nullableMax(Long current, Long received) {
    if (current == null) return received;
    if (received == null) return current;
    return Math.max(current, received);
  }

  /** Contrato de heartbeat enviado pelo executor. */
  public record HeartbeatRequest(
      Long processId,
      Boolean processAlive,
      Long eventCount,
      Long outputBytes,
      Long inputTokens,
      Long outputTokens,
      String lastEventType) {}

  /** Contrato de encerramento enviado pelo executor. */
  public record FinishRequest(
      Boolean success, Long eventCount, Long outputBytes, Long inputTokens, Long outputTokens) {}

  /** Contrato público do diagnóstico de execução. */
  public record Response(
      String agentType,
      Long executionId,
      String status,
      Long processId,
      Boolean processAlive,
      Long eventCount,
      Long outputBytes,
      Long inputTokens,
      Long outputTokens,
      String lastEventType,
      Instant lastActivityAt,
      Instant startedAt,
      Instant finishedAt,
      boolean stale) {}
}
