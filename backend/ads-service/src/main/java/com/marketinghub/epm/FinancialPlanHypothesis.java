package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Representa uma hipótese comercial com limite financeiro dentro de um nicho planejado.
 */
@Entity
@Table(name = "financial_plan_hypothesis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialPlanHypothesis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_plan_niche_id", nullable = false)
    private FinancialPlanNiche financialPlanNiche;

    @Column(name = "external_hypothesis_id", length = 36)
    private String externalHypothesisId;

    @Column(nullable = false, length = 191)
    private String title;

    @Column(name = "planned_experiments", nullable = false)
    private Integer plannedExperiments;

    @Column(name = "planned_cost_per_experiment_cents", nullable = false)
    private Long plannedCostPerExperimentCents;

    @Column(name = "loss_limit_cents", nullable = false)
    private Long lossLimitCents;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FinancialPlanHypothesisStatus status = FinancialPlanHypothesisStatus.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
