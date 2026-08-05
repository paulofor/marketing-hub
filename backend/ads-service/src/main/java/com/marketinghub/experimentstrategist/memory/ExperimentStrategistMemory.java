package com.marketinghub.experimentstrategist.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir um aprendizado comportamental estruturado e auditavel. */
@Getter
@Setter
@Entity
@Table(name = "experiment_strategist_memory")
public class ExperimentStrategistMemory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "commercial_plan_id", nullable = false)
  private Long commercialPlanId;

  @Column(name = "execution_id")
  private Long executionId;

  @Column(name = "behavioral_mechanism", nullable = false)
  private String behavioralMechanism;

  @Column(name = "statement", nullable = false, columnDefinition = "TEXT")
  private String statement;

  @Column(name = "evidence_level", nullable = false)
  private String evidenceLevel;

  @Column(name = "confidence", nullable = false)
  private String confidence;

  @Column(name = "validation_status", nullable = false)
  private String validationStatus;

  @Column(name = "source_references_json", nullable = false, columnDefinition = "LONGTEXT")
  private String sourceReferencesJson;

  @Column(name = "observed_outcome", columnDefinition = "TEXT")
  private String observedOutcome;

  @Column(name = "valid_until", nullable = false)
  private Instant validUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Define datas de auditoria na criacao. */
  @PrePersist
  void initialize() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza a data de auditoria quando o aprendizado muda. */
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
