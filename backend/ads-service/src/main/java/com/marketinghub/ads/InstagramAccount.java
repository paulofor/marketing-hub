package com.marketinghub.ads;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

@Entity(name = "ig_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Builder.Default
    private String currency = "BRL";
    private String avatarUrl;

    @Column(name = "instagram_user_id")
    private String instagramUserId;

    @Column(name = "facebook_page_id")
    private String facebookPageId;

    @Column(name = "ad_account_id")
    private String adAccountId;

    @Column(columnDefinition = "LONGTEXT")
    private String accessToken;

    @Transient
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean accessTokenProvided;

    public void setCurrency(String currency) {
        this.currency = currency == null || currency.isBlank() ? "BRL" : currency;
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
}
