package com.marketinghub.mois;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "mois_offer_funnel_pattern")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoisOfferFunnelPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_pk", nullable = false)
    private MoisDiscoveryRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_pk")
    private MoisSourceSnapshot sourceSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_card_pk", nullable = false)
    private MoisOfferCard offerCard;

    @Column(name = "artifact_id", nullable = false, unique = true, length = 64)
    private String artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MoisArtifactStatus status;

    @Column(name = "entry_asset_type", length = 128)
    private String entryAssetType;

    @Column(name = "lead_capture_fields_json", nullable = false, columnDefinition = "LONGTEXT")
    private String leadCaptureFieldsJson;

    @Column(name = "cta_style", length = 128)
    private String ctaStyle;

    @Column(name = "next_step_hypothesis", length = 255)
    private String nextStepHypothesis;

    @Column(name = "delivery_format", length = 128)
    private String deliveryFormat;

    @Column(name = "upsell_visible")
    private Boolean upsellVisible;

    @Column(name = "retention_hint", length = 255)
    private String retentionHint;

    @Column(name = "confidence")
    private Double confidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
