package com.marketinghub.ads;

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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Setter(AccessLevel.NONE)
    @Column(columnDefinition = "LONGTEXT")
    private String appSecret;

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
    @JsonProperty("hasAppSecret")
    public boolean hasAppSecret() {
        return appSecret != null && !appSecret.isBlank();
    }
}
