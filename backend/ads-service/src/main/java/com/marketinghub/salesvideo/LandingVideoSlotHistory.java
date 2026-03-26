package com.marketinghub.salesvideo;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.media.Asset;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Histórico de alterações aplicadas a um slot publicado.
 */
@Entity
@Table(name = "landing_video_slot_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingVideoSlotHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    @ToString.Exclude
    private LandingVideoSlot slot;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    @ToString.Exclude
    private SalesVideoProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_page_id")
    @ToString.Exclude
    private LandingPage landingPage;

    @Column(name = "slot_name", nullable = false, length = 64)
    private String slotName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
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

    private boolean autoplay;
    private boolean muted;
    @Column(name = "loop_video")
    private boolean loopVideo;
    @Column(name = "controls_enabled")
    private boolean controlsEnabled;
    @Column(name = "lazy_load")
    private boolean lazyLoad;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 32)
    private LandingVideoSlotChangeType changeType;

    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    private String publishedBy;
    private Instant publishedAt;

    @Lob
    private String notes;
}
