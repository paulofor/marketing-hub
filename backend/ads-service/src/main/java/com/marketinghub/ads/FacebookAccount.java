package com.marketinghub.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
