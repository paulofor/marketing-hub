package com.marketinghub.ads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity(name = "fb_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacebookAccount {
    private static final long TOKEN_RENEWAL_THRESHOLD_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String currency;

    @Column(columnDefinition = "LONGTEXT")
    private String accessToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime tokenLastRefreshedAt;
    private String authorizedUserId;
    private String authorizedUserName;
    private String authorizedUserEmail;
    private String appId;

    @Column(name = "business_manager_app_id")
    private String businessManagerAppId;

    @Column(name = "ad_account_id")
    private String adAccountId;

    @Column(name = "default_page_id")
    private String defaultPageId;

    @Column(name = "default_website_url", length = 512)
    private String defaultWebsiteUrl;

    @Column(name = "default_instagram_actor_id")
    private String defaultInstagramActorId;

    @Column(name = "default_creative_message_template")
    private String defaultCreativeMessageTemplate;

    @Column(name = "default_call_to_action_type")
    private String defaultCallToActionType;

    @Column(name = "ad_set_daily_budget")
    private String adSetDailyBudget;

    @Column(name = "ad_set_billing_event")
    private String adSetBillingEvent;

    @Column(name = "ad_set_optimization_goal")
    private String adSetOptimizationGoal;

    @Column(name = "ad_set_destination_type")
    private String adSetDestinationType;

    @Column(name = "ad_set_bid_strategy")
    private String adSetBidStrategy;

    @Column(name = "ad_set_bid_amount")
    private String adSetBidAmount;

    @Column(name = "ad_set_target_country")
    private String adSetTargetCountry;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Setter(AccessLevel.NONE)
    @Column(columnDefinition = "LONGTEXT")
    private String appSecret;

    @Column(name = "worker_enabled")
    private boolean workerEnabled;

    private boolean tokenRenewalEnabled;
    private String tokenRenewalStatus;
    private LocalDateTime tokenRenewalLastAttemptAt;
    private LocalDateTime tokenRenewedAt;

    @Column(columnDefinition = "LONGTEXT")
    private String tokenRenewalLastError;

    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean appSecretProvided;

    @Transient
    @JsonProperty("tokenExpired")
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }

    @Transient
    @JsonProperty("requiresTokenRenewal")
    public boolean isTokenRenewalRequired() {
        if (accessToken == null || accessToken.isBlank()) {
            return true;
        }
        if (tokenExpiresAt == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(TOKEN_RENEWAL_THRESHOLD_DAYS);
        return !tokenExpiresAt.isAfter(threshold);
    }

    @Transient
    @JsonProperty("tokenExpiresInDays")
    public Long getTokenExpiresInDays() {
        if (tokenExpiresAt == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), tokenExpiresAt);
        return days;
    }

    @JsonSetter("appSecret")
    public void jsonAppSecretSetter(String appSecret) {
        this.appSecret = appSecret;
        this.appSecretProvided = true;
    }

    public void overwriteAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    @Transient
    @JsonIgnore
    public boolean isAppSecretProvided() {
        return appSecretProvided;
    }

    @Transient
    @JsonGetter("hasAppSecret")
    @JsonProperty(value = "hasAppSecret", access = JsonProperty.Access.READ_ONLY)
    public boolean hasAppSecret() {
        return appSecret != null && !appSecret.isBlank();
    }
}
