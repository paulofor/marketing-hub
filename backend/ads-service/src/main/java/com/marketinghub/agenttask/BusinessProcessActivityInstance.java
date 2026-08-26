package com.marketinghub.agenttask;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: consolidar uma ocorrência auditável de atividade para uma entidade operacional.
 */
@Entity
@Table(
    name = "business_process_activity_instance",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_business_process_activity_instance",
            columnNames = {"activity_definition_id", "source_reference", "occurrence_number"}))
@Getter
@Setter
public class BusinessProcessActivityInstance {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_definition_id", nullable = false)
  private BusinessProcessActivityDefinition activityDefinition;

  @Column(name = "source_reference", nullable = false, length = 200)
  private String sourceReference;

  @Column(name = "occurrence_number", nullable = false)
  private Integer occurrenceNumber;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "entered_at", nullable = false)
  private Instant enteredAt;

  @Column(name = "exited_at")
  private Instant exitedAt;

  @Column(name = "objective_achieved", nullable = false)
  private boolean objectiveAchieved;

  @Column(name = "objective_evidence_json", columnDefinition = "LONGTEXT")
  private String objectiveEvidenceJson;

  @Column(name = "blocked_reason", columnDefinition = "LONGTEXT")
  private String blockedReason;

  @Column(name = "known_cost_usd", precision = 18, scale = 8)
  private BigDecimal knownCostUsd;

  @Column(name = "cost_coverage", nullable = false, length = 32)
  private String costCoverage;

  @Column(name = "evidence_quality", nullable = false, length = 32)
  private String evidenceQuality;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
