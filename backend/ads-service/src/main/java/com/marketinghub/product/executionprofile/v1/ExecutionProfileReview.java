package com.marketinghub.product.executionprofile.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: manter decisões financeiras auditáveis por checkpoint e revisão da ficha. */
@Entity
@Table(name = "product_execution_review_v1")
@Getter
@Setter
public class ExecutionProfileReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "profile_id", nullable = false)
  private Long profileId;

  @Column(name = "checkpoint", nullable = false, length = 32)
  private String checkpoint;

  @Column(name = "financial_execution_id", nullable = false)
  private Long financialExecutionId;

  @Column(name = "approved", nullable = false)
  private boolean approved;

  @Column(name = "reviewed_by", nullable = false, length = 100)
  private String reviewedBy;

  @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
  private String rationale;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
