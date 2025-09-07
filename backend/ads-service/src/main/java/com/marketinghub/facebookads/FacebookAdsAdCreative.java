package com.marketinghub.facebookads;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "facebook_ads_ad_creative")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsAdCreative {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "page_id", nullable = false)
    private String pageId;

    @Column(name = "instagram_user_id")
    private String instagramUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdCreativeKind kind;

    @Column(name = "link_data_json", columnDefinition = "LONGTEXT")
    private String linkDataJson;

    @Column(name = "video_data_json", columnDefinition = "LONGTEXT")
    private String videoDataJson;

    @Column(name = "carousel_data_json", columnDefinition = "LONGTEXT")
    private String carouselDataJson;

    @Column(name = "last_preview_url")
    private String lastPreviewUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
