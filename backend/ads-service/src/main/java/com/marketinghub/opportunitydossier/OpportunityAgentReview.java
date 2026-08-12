package com.marketinghub.opportunitydossier;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: persistir a solicitação e o parecer independente de um agente. */
@Entity
@Table(
    name = "opportunity_agent_review",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_opportunity_review_agent",
            columnNames = {"dossier_id", "agent_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityAgentReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dossier_id", nullable = false)
  private OpportunityDossier dossier;

  @Column(name = "agent_key", nullable = false, length = 64)
  private String agentKey;

  @Enumerated(EnumType.STRING)
  @Column(length = 24)
  private OpportunityReviewDecision decision;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(columnDefinition = "LONGTEXT")
  private String rationale;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(columnDefinition = "LONGTEXT")
  private String risks;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(columnDefinition = "LONGTEXT")
  private String recommendation;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "execution_status", nullable = false, length = 24)
  @Builder.Default
  private OpportunityReviewExecutionStatus executionStatus =
      OpportunityReviewExecutionStatus.PENDING;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "error_message", columnDefinition = "LONGTEXT")
  private String errorMessage;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "raw_model_response", columnDefinition = "LONGTEXT")
  private String rawModelResponse;

  @Column(name = "model_name", length = 191)
  private String modelName;

  /** Inicializa a auditoria operacional do parecer. */
  @PrePersist
  void initializeExecutionAudit() {
    updatedAt = Instant.now();
  }

  /** Atualiza a última atividade persistida do parecer. */
  @PreUpdate
  void updateExecutionAudit() {
    updatedAt = Instant.now();
  }
}
