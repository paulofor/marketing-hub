package com.marketinghub.facebookads;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa a campanha publicada na Meta para um experimento do Marketing Hub.
 */
@Entity
@Table(name = "facebook_ads_campaign")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsCampaign {
    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "ad_account_id", nullable = false)
    private String adAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facebook_account_id", nullable = false)
    private FacebookAccount facebookAccount;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacebookAdStatus status = FacebookAdStatus.PAUSED;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_mode", nullable = false)
    private BudgetMode budgetMode;

    @Column(name = "daily_budget_minor")
    private Long dailyBudgetMinor;

    @Column(name = "lifetime_budget_minor")
    private Long lifetimeBudgetMinor;

    @Column(name = "api_version")
    private String apiVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_reason", length = 100)
    private FacebookCampaignStopReason stopReason;

    @Column(name = "stop_requested_at")
    private Instant stopRequestedAt;

    @Column(name = "stop_completed_at")
    private Instant stopCompletedAt;

    @Column(name = "stop_last_error", columnDefinition = "TEXT")
    private String stopLastError;

    @ElementCollection(targetClass = SpecialAdCategory.class)
    @CollectionTable(
            name = "facebook_ads_campaign_special_ad_category",
            joinColumns = @JoinColumn(name = "campaign_id", columnDefinition = "CHAR(36)")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Set<SpecialAdCategory> specialAdCategories = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "facebook_ads_campaign_special_ad_country",
            joinColumns = @JoinColumn(name = "campaign_id", columnDefinition = "CHAR(36)")
    )
    @Column(name = "country_iso2", length = 2, nullable = false)
    private Set<String> specialAdCountries = new HashSet<>();

    @OneToMany(mappedBy = "campaign", fetch = FetchType.LAZY)
    private List<FacebookAdsAdSet> adSets = new ArrayList<>();

    @Column(name = "metrics_last_synced_at")
    private Instant metricsLastSyncedAt;

    @Column(name = "metrics_final_synced_at")
    private Instant metricsFinalSyncedAt;

    @Column(name = "metrics_last_error", columnDefinition = "TEXT")
    private String metricsLastError;

    @Column(name = "recommendations_last_synced_at")
    private Instant recommendationsLastSyncedAt;

    @Column(name = "recommendations_last_error", columnDefinition = "TEXT")
    private String recommendationsLastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
