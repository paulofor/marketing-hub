package com.marketinghub.agenttask;

import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: representar uma solicitação auditável na caixa de entrada de um agente. */
@Entity
@Table(name = "agent_task")
@Getter
@Setter
public class AgentTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assigned_agent_id", nullable = false)
  private Agent assignedAgent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by_agent_id")
  private Agent requestedByAgent;

  @Column(name = "requested_by_type", nullable = false, length = 20)
  private String requestedByType;

  @Column(name = "requested_by_name", nullable = false, length = 100)
  private String requestedByName;

  @Column(name = "title", nullable = false, length = 160)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "LONGTEXT")
  private String description;

  @Column(name = "priority", nullable = false, length = 20)
  private String priority;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "source_reference", length = 200)
  private String sourceReference;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "process_definition_id")
  private BusinessProcessDefinition processDefinition;

  @Column(name = "process_activity_id", length = 100)
  private String processActivityId;

  @Column(name = "process_activity_name", length = 160)
  private String processActivityName;

  @Column(name = "exceptional", nullable = false)
  private boolean exceptional;

  @Column(name = "exception_reason", length = 500)
  private String exceptionReason;

  @Column(name = "task_kind", nullable = false, length = 30)
  private String taskKind;

  @Column(name = "gate_code", length = 100)
  private String gateCode;

  @Column(name = "gate_status", length = 30)
  private String gateStatus;

  @Column(name = "gate_decision_reason", columnDefinition = "LONGTEXT")
  private String gateDecisionReason;

  @Column(name = "gate_decided_at")
  private Instant gateDecidedAt;

  @Column(name = "received_at")
  private Instant receivedAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "result_json", columnDefinition = "LONGTEXT")
  private String resultJson;

  @Column(name = "evidence_json", columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "execution_error", columnDefinition = "LONGTEXT")
  private String executionError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
