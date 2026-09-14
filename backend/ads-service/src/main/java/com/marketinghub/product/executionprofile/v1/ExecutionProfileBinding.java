package com.marketinghub.product.executionprofile.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: congelar a ficha de uma referência de execução sem reescrever históricos. */
@Entity
@Table(name = "product_execution_binding_v1")
@Getter
@Setter
public class ExecutionProfileBinding {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "profile_id", nullable = false)
  private Long profileId;

  @Column(name = "source_reference", nullable = false, length = 191)
  private String sourceReference;

  @Column(name = "learning_cycle_id")
  private Long learningCycleId;

  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
