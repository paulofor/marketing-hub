package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Responsabilidade: auditar uma reconexão Codex sem persistir qualquer credencial OAuth. */
@Entity
@Table(name = "agent_codex_auth_reconnect")
@Getter
@NoArgsConstructor
public class CodexAuthReconnect {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_id", nullable = false)
  private Agent agent;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "verification_url", length = 500)
  private String verificationUrl;

  @Column(name = "user_code", length = 30)
  private String userCode;

  @Column(name = "requested_by", nullable = false, length = 100)
  private String requestedBy;

  @Column(name = "detail", length = 500)
  private String detail;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  /** Cria uma solicitação aguardando coleta pelo executor do agente. */
  public CodexAuthReconnect(Agent agent, String requestedBy, Instant requestedAt) {
    this.agent = agent;
    this.status = "REQUESTED";
    this.requestedBy = requestedBy;
    this.requestedAt = requestedAt;
  }

  /** Reserva a solicitação para uma única execução operacional. */
  public void start(Instant now) {
    status = "STARTING";
    startedAt = now;
  }

  /** Publica somente os dados temporários que o operador precisa confirmar. */
  public void awaitConfirmation(String verificationUrl, String userCode) {
    if (!"STARTING".equals(status))
      throw new IllegalStateException("Reconexão não está aguardando o device code.");
    this.verificationUrl = verificationUrl;
    this.userCode = userCode;
    this.status = "AWAITING_CONFIRMATION";
  }

  /** Finaliza a auditoria sem receber ou armazenar tokens. */
  public void finish(boolean authenticated, String detail, Instant now) {
    if (!("STARTING".equals(status) || "AWAITING_CONFIRMATION".equals(status)))
      throw new IllegalStateException("Reconexão não está ativa.");
    status = authenticated ? "AUTHENTICATED" : "FAILED";
    this.detail = detail;
    completedAt = now;
    userCode = null;
  }
}
