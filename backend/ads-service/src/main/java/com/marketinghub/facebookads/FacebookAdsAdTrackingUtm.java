package com.marketinghub.facebookads;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "facebook_ads_ad_tracking_utm")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsAdTrackingUtm {
    @Id
    @Column(name = "ad_id", length = 36)
    private String adId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "ad_id")
    private FacebookAdsAd ad;

    @Column(name = "utm_source")
    private String utmSource;

    @Column(name = "utm_medium")
    private String utmMedium;

    @Column(name = "utm_campaign")
    private String utmCampaign;

    @Column(name = "utm_content")
    private String utmContent;

    @Column(name = "utm_term")
    private String utmTerm;
}
