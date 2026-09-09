package com.marketinghub.businessprocesschain.learningcycle.v1.decision;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar cada tentativa de proposta de Atena e sua aprovação humana. */
@Entity
@Table(
    name = "learning_cycle_decision_proposal_v1",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "cycle_revision", "attempt"}))
@Getter
@Setter
public class LearningCycleDecisionProposal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "cycle_revision", nullable = false)
  private long cycleRevision;

  @Column(name = "attempt", nullable = false)
  private int attempt;

  @Column(name = "activity_definition_id", nullable = false)
  private Long activityDefinitionId;

  @Column(name = "activity_instance_id", nullable = false)
  private Long activityInstanceId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "lease_token", length = 36)
  private String leaseToken;

  @Column(name = "context_json", nullable = false, columnDefinition = "LONGTEXT")
  private String contextJson;

  @Column(name = "audit_json", columnDefinition = "LONGTEXT")
  private String auditJson;

  @Column(name = "raw_response", columnDefinition = "LONGTEXT")
  private String rawResponse;

  @Column(name = "result_receipt_json", columnDefinition = "LONGTEXT")
  private String resultReceiptJson;

  @Column(name = "proposal_json", columnDefinition = "LONGTEXT")
  private String proposalJson;

  @Column(name = "error", columnDefinition = "TEXT")
  private String error;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approved_event_id")
  private Long approvedEventId;
}
