package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Representa um nicho planejado dentro de um plano financeiro do EPM.
 */
@Entity
@Table(name = "financial_plan_niche")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialPlanNiche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_plan_id", nullable = false)
    private FinancialPlan financialPlan;

    @Column(name = "external_niche_id")
    private Long externalNicheId;

    @Column(name = "niche_name", nullable = false, length = 191)
    private String nicheName;

    @Column(name = "planned_budget_cents", nullable = false)
    private Long plannedBudgetCents;

    @Column(name = "spend_limit_cents", nullable = false)
    private Long spendLimitCents;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FinancialPlanNicheStatus status = FinancialPlanNicheStatus.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
