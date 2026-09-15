package com.marketinghub.financialplan.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: guardar uma revisão financeira imutável e seu vínculo único com Plutus. */
@Entity
@Table(name = "product_financial_plan_v1")
@Getter
@Setter
public class FinancialPlanRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scope_kind", nullable = false, length = 20)
  private String scopeKind;

  @Column(name = "scope_id", nullable = false)
  private Long scopeId;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "product_type_id")
  private Long productTypeId;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Column(name = "environment", nullable = false, length = 10)
  private Environment environment;

  @Column(name = "revision_number", nullable = false)
  private int revisionNumber;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "template_id")
  private Long templateId;

  @Column(name = "commercial_plan_id")
  private Long commercialPlanId;

  @Column(name = "commercial_plan_version")
  private Integer commercialPlanVersion;

  @Column(name = "assumptions_json", nullable = false, columnDefinition = "LONGTEXT")
  private String assumptionsJson;

  @Column(name = "evaluation_json", nullable = false, columnDefinition = "LONGTEXT")
  private String evaluationJson;

  @Column(name = "financial_execution_id")
  private Long financialExecutionId;

  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Separa hipóteses de homologação das revisões da operação real. */
  public enum Environment {
    LIVE,
    TEST
  }
}
