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

/** Responsabilidade: preservar cada alteração auditável do controle automático de um agente. */
@Entity
@Table(name = "agent_automatic_execution_control_event")
@Getter
@NoArgsConstructor
public class AgentAutomaticExecutionControlEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_id", nullable = false)
  private Agent agent;

  @Column(name = "automatic_execution_enabled", nullable = false)
  private boolean automaticExecutionEnabled;

  @Column(name = "changed_by", nullable = false, length = 100)
  private String changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** Cria um evento imutável com a decisão operacional e sua autoria. */
  public AgentAutomaticExecutionControlEvent(
      Agent agent, boolean automaticExecutionEnabled, String changedBy, Instant changedAt) {
    this.agent = agent;
    this.automaticExecutionEnabled = automaticExecutionEnabled;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
  }
}
