package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Consolida métricas financeiras manuais observadas em um experimento do EPM.
 */
@Entity
@Table(name = "experiment_financial_metric")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentFinancialMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_budget_id", nullable = false)
    private ExperimentBudget experimentBudget;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(nullable = false)
    private Integer visitors;

    @Column(nullable = false)
    private Integer leads;

    @Column(name = "checkout_clicks", nullable = false)
    private Integer checkoutClicks;

    @Column(nullable = false)
    private Integer purchases;

    @Column(name = "ad_spend_cents", nullable = false)
    private Long adSpendCents;

    @Column(name = "revenue_cents", nullable = false)
    private Long revenueCents;

    @Column(name = "payment_fee_cents", nullable = false)
    private Long paymentFeeCents;

    @Column(name = "platform_fee_cents", nullable = false)
    private Long platformFeeCents;

    @Column(name = "ai_cost_cents", nullable = false)
    private Long aiCostCents;

    @Column(name = "tax_estimate_cents", nullable = false)
    private Long taxEstimateCents;

    @Column(name = "gross_profit_cents", nullable = false)
    private Long grossProfitCents;

    @Column(name = "estimated_net_profit_cents", nullable = false)
    private Long estimatedNetProfitCents;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
