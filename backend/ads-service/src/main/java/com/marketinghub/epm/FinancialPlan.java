package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Representa o plano financeiro de um período para controlar a verba total de experimentos do EPM.
 */
@Entity
@Table(name = "financial_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 191)
    private String name;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date", nullable = false)
    private LocalDate cycleEndDate;

    @Column(name = "total_budget_cents", nullable = false)
    private Long totalBudgetCents;

    @Column(name = "default_daily_budget_cents")
    private Long defaultDailyBudgetCents;

    @Column(name = "default_experiment_duration_days")
    private Integer defaultExperimentDurationDays;

    @Column(name = "default_experiments_per_hypothesis")
    private Integer defaultExperimentsPerHypothesis;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FinancialPlanStatus status = FinancialPlanStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
