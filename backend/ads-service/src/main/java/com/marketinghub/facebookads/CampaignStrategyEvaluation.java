package com.marketinghub.facebookads;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registra uma avaliacao auditavel feita pela estrategia de campanha.
 */
@Entity
@Table(name = "campaign_strategy_evaluation")
@Getter
@Setter
@NoArgsConstructor
public class CampaignStrategyEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_id", nullable = false)
    private CampaignStrategy strategy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, columnDefinition = "CHAR(36)")
    private FacebookAdsCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private CampaignStrategyDecision decision;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "spend", precision = 12, scale = 2)
    private BigDecimal spend;

    @Column(name = "impressions")
    private Long impressions;

    @Column(name = "clicks")
    private Long clicks;

    @Column(name = "checkout_attempts")
    private Long checkoutAttempts;

    @Column(name = "checkout_clicks")
    private Long checkoutClicks;

    @Column(name = "purchases")
    private Long purchases;

    @Column(name = "checkout_rate", precision = 7, scale = 4)
    private BigDecimal checkoutRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_reason", length = 100)
    private FacebookCampaignStopReason stopReason;

    @CreationTimestamp
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;
}
