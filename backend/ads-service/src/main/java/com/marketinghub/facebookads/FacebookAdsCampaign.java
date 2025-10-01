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
import java.util.HashSet;
import java.util.Set;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
