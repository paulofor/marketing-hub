package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Responsabilidade: registrar um comando administrativo auditável destinado ao host dos agentes.
 */
@Entity
@Table(name = "agent_executor_admin_operation")
@Getter
@NoArgsConstructor
public class AgentExecutorAdminOperation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_id", nullable = false)
  private Agent agent;

  @Column(name = "operation_type", nullable = false, length = 20)
  private String operationType;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "requested_by", nullable = false, length = 100)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "detail", length = 500)
  private String detail;

  /** Cria uma solicitação que ainda não causou efeito externo. */
  public AgentExecutorAdminOperation(
      Agent agent, String operationType, String requestedBy, Instant requestedAt) {
    this.agent = agent;
    this.operationType = operationType;
    this.status = "REQUESTED";
    this.requestedBy = requestedBy;
    this.requestedAt = requestedAt;
  }

  /** Reserva o comando para um único controlador de host. */
  public void start(Instant at) {
    if (!"REQUESTED".equals(status)) throw new IllegalStateException("Operação não está pendente.");
    status = "RUNNING";
    startedAt = at;
  }

  /** Registra o resultado informado pelo controlador sem inferir prontidão. */
  public void complete(boolean success, String resultDetail, Instant at) {
    if (!"RUNNING".equals(status))
      throw new IllegalStateException("Operação não está em execução.");
    status = success ? "COMPLETED" : "FAILED";
    detail = resultDetail;
    completedAt = at;
  }
}
