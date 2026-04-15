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
    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean accessTokenProvided;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime tokenLastRefreshedAt;
    private String authorizedUserId;
    private String authorizedUserName;
    private String authorizedUserEmail;
    private String appId;

    @Column(name = "ad_account_id")
    private String adAccountId;

    @Column(name = "default_page_id")
    private String defaultPageId;
    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean defaultPageIdProvided;

    @Column(name = "default_website_url", length = 512)
    private String defaultWebsiteUrl;

    @Column(name = "default_lead_gen_form_id")
    private String defaultLeadGenFormId;
    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean defaultLeadGenFormIdProvided;

    @Column(name = "default_instagram_actor_id")
    private String defaultInstagramActorId;
    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean defaultInstagramActorIdProvided;

    @Column(name = "default_creative_message_template")
    private String defaultCreativeMessageTemplate;

    @Column(name = "default_call_to_action_type")
    private String defaultCallToActionType;

    @Column(name = "pixel_owner_business_id", length = 64)
    private String pixelOwnerBusinessId;
    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean pixelOwnerBusinessIdProvided;

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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "worker_last_validation_at")
    private LocalDateTime workerLastValidationAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "worker_last_validation_error_code", length = 128)
    private String workerLastValidationErrorCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "worker_last_validation_error_detail", length = 512)
    private String workerLastValidationErrorDetail;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Setter(AccessLevel.NONE)
    @Column(columnDefinition = "LONGTEXT")
    private String appSecret;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "system_user_access_token", columnDefinition = "LONGTEXT")
    private String systemUserAccessToken;
    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean systemUserAccessTokenProvided;

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

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @JsonSetter("accessToken")
    public void jsonAccessTokenSetter(String accessToken) {
        this.accessTokenProvided = true;
        this.accessToken = accessToken;
    }

    @Transient
    @JsonIgnore
    public boolean isAccessTokenProvided() {
        return accessTokenProvided;
    }

    public void setSystemUserAccessToken(String systemUserAccessToken) {
        this.systemUserAccessToken = systemUserAccessToken;
    }

    @JsonSetter("systemUserAccessToken")
    public void jsonSystemUserAccessTokenSetter(String systemUserAccessToken) {
        this.systemUserAccessTokenProvided = true;
        this.systemUserAccessToken = systemUserAccessToken;
    }

    @Transient
    @JsonIgnore
    public boolean isSystemUserAccessTokenProvided() {
        return systemUserAccessTokenProvided;
    }

    public void setDefaultPageId(String defaultPageId) {
        this.defaultPageId = defaultPageId;
    }

    @JsonSetter("defaultPageId")
    public void jsonDefaultPageIdSetter(String defaultPageId) {
        this.defaultPageIdProvided = true;
        this.defaultPageId = defaultPageId;
    }

    @Transient
    @JsonIgnore
    public boolean isDefaultPageIdProvided() {
        return defaultPageIdProvided;
    }

    public void setDefaultInstagramActorId(String defaultInstagramActorId) {
        this.defaultInstagramActorId = defaultInstagramActorId;
    }

    @JsonSetter("defaultInstagramActorId")
    public void jsonDefaultInstagramActorIdSetter(String defaultInstagramActorId) {
        this.defaultInstagramActorIdProvided = true;
        this.defaultInstagramActorId = defaultInstagramActorId;
    }

    @Transient
    @JsonIgnore
    public boolean isDefaultInstagramActorIdProvided() {
        return defaultInstagramActorIdProvided;
    }

    public void setPixelOwnerBusinessId(String pixelOwnerBusinessId) {
        this.pixelOwnerBusinessId = pixelOwnerBusinessId;
    }

    @JsonSetter("pixelOwnerBusinessId")
    public void jsonPixelOwnerBusinessIdSetter(String pixelOwnerBusinessId) {
        this.pixelOwnerBusinessIdProvided = true;
        this.pixelOwnerBusinessId = pixelOwnerBusinessId;
    }

    @Transient
    @JsonIgnore
    public boolean isPixelOwnerBusinessIdProvided() {
        return pixelOwnerBusinessIdProvided;
    }

    @JsonSetter("defaultLeadGenFormId")
    public void jsonDefaultLeadGenFormIdSetter(String defaultLeadGenFormId) {
        this.defaultLeadGenFormIdProvided = true;
        this.defaultLeadGenFormId = defaultLeadGenFormId;
    }

    @Transient
    @JsonIgnore
    public boolean isDefaultLeadGenFormIdProvided() {
        return defaultLeadGenFormIdProvided;
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
