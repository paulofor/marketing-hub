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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Define a estrategia de decisao automatica que controla a utilidade de uma campanha Meta.
 */
@Entity
@Table(name = "campaign_strategy")
@Getter
@Setter
@NoArgsConstructor
public class CampaignStrategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private FacebookAdsCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "objective", nullable = false, length = 64)
    private CampaignStrategyObjective objective;

    @Column(name = "preset", nullable = false, length = 64)
    private String preset;

    @Column(name = "max_spend_without_purchase", precision = 12, scale = 2)
    private BigDecimal maxSpendWithoutPurchase;

    @Column(name = "minimum_checkout_rate", precision = 7, scale = 4)
    private BigDecimal minimumCheckoutRate;

    @Column(name = "minimum_link_clicks")
    private Long minimumLinkClicks;

    @Column(name = "minimum_impressions")
    private Long minimumImpressions;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
