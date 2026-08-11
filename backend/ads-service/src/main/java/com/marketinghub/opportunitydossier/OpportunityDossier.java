package com.marketinghub.opportunitydossier;

import com.marketinghub.planning.CommercialPlan;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: persistir a oportunidade validável que antecede um plano comercial. */
@Entity
@Table(name = "opportunity_dossier")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityDossier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 191)
  private String title;

  @Column(name = "owner_agent_key", nullable = false, length = 64)
  private String ownerAgentKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  @Builder.Default
  private OpportunityDossierStatus status = OpportunityDossierStatus.RESEARCHING;

  @Column(name = "target_audience", nullable = false, length = 512)
  private String targetAudience;

  @Column(name = "main_pain", nullable = false, length = 512)
  private String mainPain;

  @Column(name = "reference_product", nullable = false, length = 512)
  private String referenceProduct;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "ai_advantage", nullable = false, columnDefinition = "LONGTEXT")
  private String aiAdvantage;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "proposed_offer", columnDefinition = "LONGTEXT")
  private String proposedOffer;

  @Column(name = "preliminary_price", precision = 12, scale = 2)
  private BigDecimal preliminaryPrice;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "delivery_model", columnDefinition = "LONGTEXT")
  private String deliveryModel;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "known_risks", columnDefinition = "LONGTEXT")
  private String knownRisks;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "experiment_recommendation", columnDefinition = "LONGTEXT")
  private String experimentRecommendation;

  @Column(name = "human_decision_by", length = 191)
  private String humanDecisionBy;

  @Column(name = "human_decision_at")
  private Instant humanDecisionAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "converted_plan_id")
  private CommercialPlan convertedPlan;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
