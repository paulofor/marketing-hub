package com.marketinghub.facebookads;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "facebook_ads_ad_set")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsAdSet {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "external_id")
    private String externalId;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private FacebookAdsCampaign campaign;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacebookAdStatus status = FacebookAdStatus.PAUSED;

    @Column(name = "daily_budget_minor")
    private Long dailyBudgetMinor;

    @Column(name = "lifetime_budget_minor")
    private Long lifetimeBudgetMinor;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "billing_event", nullable = false)
    private String billingEvent;

    @Column(name = "optimization_goal", nullable = false)
    private String optimizationGoal;

    @Column(name = "bid_strategy", nullable = false)
    private String bidStrategy;

    @Column(name = "bid_amount_minor")
    private Long bidAmountMinor;

    @Column(name = "promoted_object_json", columnDefinition = "LONGTEXT")
    private String promotedObjectJson;

    @Column(name = "targeting_json", nullable = false, columnDefinition = "LONGTEXT")
    private String targetingJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
