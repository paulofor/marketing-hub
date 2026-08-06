package com.marketinghub.codextelemetry;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir sinais auditáveis de progresso de uma execução Codex. */
@Getter
@Setter
@Entity
@Table(name = "codex_agent_execution_telemetry")
public class CodexAgentExecutionTelemetry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "agent_type", nullable = false, length = 64)
  private String agentType;

  @Column(name = "execution_id", nullable = false)
  private Long executionId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "process_id")
  private Long processId;

  @Column(name = "process_alive", nullable = false)
  private Boolean processAlive;

  @Column(name = "event_count", nullable = false)
  private Long eventCount;

  @Column(name = "output_bytes", nullable = false)
  private Long outputBytes;

  @Column(name = "input_tokens")
  private Long inputTokens;

  @Column(name = "output_tokens")
  private Long outputTokens;

  @Column(name = "last_event_type", length = 64)
  private String lastEventType;

  @Column(name = "last_activity_at")
  private Instant lastActivityAt;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa os campos de auditoria. */
  @PrePersist
  void initialize() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o horário da última gravação. */
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
