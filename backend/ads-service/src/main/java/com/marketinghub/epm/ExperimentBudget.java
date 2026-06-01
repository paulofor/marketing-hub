package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Representa a verba planejada para executar um experimento financeiro do EPM.
 */
@Entity
@Table(name = "experiment_budget")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_plan_hypothesis_id", nullable = false)
    private FinancialPlanHypothesis financialPlanHypothesis;

    @Column(name = "external_experiment_id")
    private Long externalExperimentId;

    @Column(nullable = false, length = 191)
    private String name;

    @Column(name = "planned_daily_budget_cents", nullable = false)
    private Long plannedDailyBudgetCents;

    @Column(name = "planned_duration_days", nullable = false)
    private Integer plannedDurationDays;

    @Column(name = "planned_total_budget_cents", nullable = false)
    private Long plannedTotalBudgetCents;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExperimentBudgetStatus status = ExperimentBudgetStatus.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
