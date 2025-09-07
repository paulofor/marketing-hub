package com.marketinghub.facebookads;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "facebook_ads_media_asset")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsMediaAsset {
    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaAssetKind kind;

    @Column(name = "source_uri")
    private String sourceUri;

    @Column(name = "image_hash")
    private String imageHash;

    @Column(name = "video_id")
    private String videoId;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column
    private String checksum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
