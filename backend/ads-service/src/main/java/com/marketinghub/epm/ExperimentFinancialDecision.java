package com.marketinghub.epm;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Registra uma decisão financeira tomada sobre um experimento ou hipótese acompanhada pelo EPM.
 */
@Entity
@Table(name = "experiment_financial_decision")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentFinancialDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_budget_id", nullable = false)
    private ExperimentBudget experimentBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 64)
    private ExperimentFinancialDecisionType decisionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "decided_by", length = 191)
    private String decidedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
