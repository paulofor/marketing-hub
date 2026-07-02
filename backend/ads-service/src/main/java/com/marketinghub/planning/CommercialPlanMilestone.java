package com.marketinghub.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: representar um marco de negocio dentro de um plano comercial. */
@Entity
@Table(name = "commercial_plan_milestone")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialPlanMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private CommercialPlan plan;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 191)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommercialPlanMilestoneStatus status = CommercialPlanMilestoneStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "target_cost", precision = 12, scale = 2)
    private BigDecimal targetCost;

    @Column(name = "target_revenue", precision = 12, scale = 2)
    private BigDecimal targetRevenue;

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

    @Column(name = "evidence_source", length = 512)
    private String evidenceSource;

    @Column(length = 512)
    private String blocker;

    @Column(name = "recommended_next_action", length = 512)
    private String recommendedNextAction;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
