package com.marketinghub.planning;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: representar o plano comercial que conecta pipelines ao objetivo de venda. */
@Entity
@Table(name = "commercial_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 191)
  private String name;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "plan_type", nullable = false, length = 32)
  private CommercialPlanType planType = CommercialPlanType.FIRST_SALE;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private CommercialPlanStatus status = CommercialPlanStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "niche_id")
  private MarketNiche niche;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hypothesis_id")
  private Hypothesis hypothesis;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id")
  private Experiment experiment;

  @Builder.Default
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "commercial_plan_experiment",
      joinColumns = @JoinColumn(name = "commercial_plan_id"),
      inverseJoinColumns = @JoinColumn(name = "experiment_id"))
  private Set<Experiment> experiments = new LinkedHashSet<>();

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "commercial_objective", columnDefinition = "LONGTEXT")
  private String commercialObjective;

  @Column(name = "target_audience", length = 512)
  private String targetAudience;

  @Column(name = "main_pain", length = 512)
  private String mainPain;

  @Column(name = "main_offer", length = 512)
  private String mainOffer;

  @Column(name = "main_lead_magnet", length = 512)
  private String mainLeadMagnet;

  @Column(name = "main_channel", length = 191)
  private String mainChannel;

  @Column(name = "main_metric", length = 191)
  private String mainMetric;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "success_criteria", columnDefinition = "LONGTEXT")
  private String successCriteria;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "stop_criteria", columnDefinition = "LONGTEXT")
  private String stopCriteria;

  @Column(name = "deadline")
  private LocalDate deadline;

  @Column(name = "max_budget", precision = 12, scale = 2)
  private BigDecimal maxBudget;

  @Column(name = "target_revenue", precision = 12, scale = 2)
  private BigDecimal targetRevenue;

  @Column(name = "operational_revenue_target", precision = 12, scale = 2)
  private BigDecimal operationalRevenueTarget;

  @Column(name = "offer_price_brl", precision = 12, scale = 2)
  private BigDecimal offerPriceBrl;

  @Column(name = "variable_cost_per_sale_brl", precision = 12, scale = 2)
  private BigDecimal variableCostPerSaleBrl;

  @Column(name = "expected_monthly_traffic")
  private Integer expectedMonthlyTraffic;

  @Column(name = "expected_conversion_rate_percent", precision = 7, scale = 4)
  private BigDecimal expectedConversionRatePercent;

  @Column(name = "expected_cac_brl", precision = 12, scale = 2)
  private BigDecimal expectedCacBrl;

  @Column(name = "expected_refund_rate_percent", precision = 7, scale = 4)
  private BigDecimal expectedRefundRatePercent;

  @Column(name = "fixed_operational_cost_brl", precision = 12, scale = 2)
  private BigDecimal fixedOperationalCostBrl;

  @Column(name = "experiments_to_create")
  private Integer experimentsToCreate;

  @Column(name = "experiments_to_publish")
  private Integer experimentsToPublish;

  @Column(name = "products_to_validate")
  private Integer productsToValidate;

  @Column(name = "product_types_to_explore")
  private Integer productTypesToExplore;

  @Column(name = "approaches_to_test")
  private Integer approachesToTest;

  @Column(name = "customer_conversations_target")
  private Integer customerConversationsTarget;

  @Column(name = "actual_campaign_cost", precision = 12, scale = 2)
  private BigDecimal actualCampaignCost;

  @Column(name = "actual_ai_cost", precision = 12, scale = 2)
  private BigDecimal actualAiCost;

  @Column(name = "actual_total_cost", precision = 12, scale = 2)
  private BigDecimal actualTotalCost;

  @Column(name = "actual_revenue", precision = 12, scale = 2)
  private BigDecimal actualRevenue;

  @Column(name = "actual_experiments_created")
  private Integer actualExperimentsCreated;

  @Column(name = "actual_experiments_published")
  private Integer actualExperimentsPublished;

  @Column(name = "execution_synced_at")
  private Instant executionSyncedAt;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "next_action", columnDefinition = "LONGTEXT")
  private String nextAction;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "current_blocker", columnDefinition = "LONGTEXT")
  private String currentBlocker;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "root_cause", columnDefinition = "LONGTEXT")
  private String rootCause;

  @Column(name = "most_likely_scenario", length = 512)
  private String mostLikelyScenario;

  @Column(name = "main_future_risk", length = 512)
  private String mainFutureRisk;

  @Column(name = "action_to_avoid", length = 512)
  private String actionToAvoid;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
