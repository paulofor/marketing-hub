package com.marketinghub.facebookads;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "facebook_ads_ad")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsAd {
    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "external_id")
    private String externalId;

    @ManyToOne
    @JoinColumn(name = "adset_id", nullable = false, columnDefinition = "CHAR(36)")
    private FacebookAdsAdSet adSet;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "creative_id", nullable = false, columnDefinition = "CHAR(36)")
    private FacebookAdsAdCreative creative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacebookAdStatus status = FacebookAdStatus.PAUSED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToOne(mappedBy = "ad", cascade = CascadeType.ALL)
    private FacebookAdsAdTrackingUtm trackingUtm;
}
