package com.marketinghub.salesvideo;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.media.Asset;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Configuração de publicação de um vídeo em uma landing page.
 */
@Entity
@Table(name = "landing_video_slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingVideoSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landing_page_id", nullable = false)
    @ToString.Exclude
    private LandingPage landingPage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    private SalesVideoProfile profile;

    @Column(name = "slot_name", nullable = false, length = 64)
    private String slotName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    @ToString.Exclude
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_asset_id")
    @ToString.Exclude
    private Asset posterAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vtt_asset_id")
    @ToString.Exclude
    private Asset vttAsset;

    @Builder.Default
    private boolean autoplay = true;
    @Builder.Default
    private boolean muted = true;
    @Builder.Default
    @Column(name = "loop_video")
    private boolean loopVideo = false;
    @Builder.Default
    @Column(name = "controls_enabled")
    private boolean controlsEnabled = true;
    @Column(name = "lazy_load")
    @Builder.Default
    private boolean lazyLoad = true;

    private Instant publishedAt;
    private String publishedBy;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
