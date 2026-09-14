package com.marketinghub.product.executionprofile.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir a revisão imutável do contrato e da composição selecionada. */
@Entity
@Table(name = "product_execution_profile_v1")
@Getter
@Setter
public class ExecutionProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "revision_number", nullable = false)
  private int revisionNumber;

  @Column(name = "chain_id", nullable = false)
  private Long chainId;

  @Column(name = "commercial_plan_id", nullable = false)
  private Long commercialPlanId;

  @Column(name = "commercial_plan_version", nullable = false)
  private int commercialPlanVersion;

  @Column(name = "product_type", length = 191)
  private String productType;

  @Column(name = "product_format", length = 191)
  private String productFormat;

  @Column(name = "contract_json", nullable = false, columnDefinition = "LONGTEXT")
  private String contractJson;

  @Column(name = "composition_json", nullable = false, columnDefinition = "LONGTEXT")
  private String compositionJson;

  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
