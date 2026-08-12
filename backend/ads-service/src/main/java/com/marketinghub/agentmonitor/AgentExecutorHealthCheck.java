package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Responsabilidade: persistir a última prova operacional enviada pelo executor de um agente. */
@Entity
@Table(name = "agent_executor_health_check")
@Getter
@NoArgsConstructor
public class AgentExecutorHealthCheck {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_id", nullable = false)
  private Agent agent;

  @Column(name = "deployed_version", nullable = false)
  private Integer deployedVersion;

  @Column(name = "build_reference", length = 100)
  private String buildReference;

  @Column(name = "backend_accessible", nullable = false)
  private boolean backendAccessible;

  @Column(name = "codex_authenticated", nullable = false)
  private boolean codexAuthenticated;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "detail", length = 500)
  private String detail;

  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;

  /** Cria uma leitura imutável com os três sinais comprovados pelo executor. */
  public AgentExecutorHealthCheck(
      Agent agent,
      Integer deployedVersion,
      String buildReference,
      boolean backendAccessible,
      boolean codexAuthenticated,
      String status,
      String detail,
      Instant checkedAt) {
    this.agent = agent;
    this.deployedVersion = deployedVersion;
    this.buildReference = buildReference;
    this.backendAccessible = backendAccessible;
    this.codexAuthenticated = codexAuthenticated;
    this.status = status;
    this.detail = detail;
    this.checkedAt = checkedAt;
  }
}
