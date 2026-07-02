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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "experiments_to_create")
    private Integer experimentsToCreate;

    @Column(name = "experiments_to_publish")
    private Integer experimentsToPublish;

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

    @Column(name = "next_action", length = 512)
    private String nextAction;

    @Column(name = "current_blocker", length = 512)
    private String currentBlocker;

    @Column(name = "root_cause", length = 512)
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
