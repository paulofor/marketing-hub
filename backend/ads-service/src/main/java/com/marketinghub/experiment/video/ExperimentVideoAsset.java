package com.marketinghub.experiment.video;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.media.Asset;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Representa um vídeo vinculado a um experimento como artefato mensurável de funil.
 */
@Entity
@Table(name = "experiment_video_asset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentVideoAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 32)
    private ExperimentVideoSlot slot;

    @Column(name = "objective", nullable = false, length = 512)
    private String objective;

    @Column(name = "primary_metric", nullable = false, length = 191)
    private String primaryMetric;

    @Column(name = "script", columnDefinition = "LONGTEXT")
    private String script;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "model", nullable = false, length = 128)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExperimentVideoStatus status;

    @Column(name = "asset_url", length = 1024)
    private String assetUrl;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "aspect_ratio", length = 16)
    private String aspectRatio;

    @Column(name = "request_json", columnDefinition = "LONGTEXT")
    private String requestJson;

    @Column(name = "response_json", columnDefinition = "LONGTEXT")
    private String responseJson;

    @Column(name = "cost", precision = 12, scale = 4)
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private ExperimentVideoReviewStatus reviewStatus;

    @Column(name = "required_for_release", nullable = false)
    private boolean requiredForRelease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_video_profile_id")
    @ToString.Exclude
    private SalesVideoProfile salesVideoProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_video_job_id")
    @ToString.Exclude
    private SalesVideoJob salesVideoJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    @ToString.Exclude
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_video_slot_id")
    @ToString.Exclude
    private LandingVideoSlot landingVideoSlot;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
