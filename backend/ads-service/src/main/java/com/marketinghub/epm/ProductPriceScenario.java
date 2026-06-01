package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Representa uma simulação de preço e ponto de equilíbrio para o plano financeiro do EPM.
 */
@Entity
@Table(name = "product_price_scenario")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceScenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_plan_id", nullable = false)
    private FinancialPlan financialPlan;

    @Column(nullable = false, length = 191)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "expected_payment_fee_cents", nullable = false)
    private Long expectedPaymentFeeCents;

    @Column(name = "expected_platform_fee_cents", nullable = false)
    private Long expectedPlatformFeeCents;

    @Column(name = "expected_tax_cents", nullable = false)
    private Long expectedTaxCents;

    @Column(name = "expected_net_revenue_per_sale_cents", nullable = false)
    private Long expectedNetRevenuePerSaleCents;

    @Column(name = "total_budget_cents", nullable = false)
    private Long totalBudgetCents;

    @Column(name = "break_even_sales", nullable = false)
    private Integer breakEvenSales;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
