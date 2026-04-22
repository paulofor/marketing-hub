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
@Table(name = "mois_offer_card")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoisOfferCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_pk", nullable = false)
    private MoisDiscoveryRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_pk")
    private MoisSourceSnapshot sourceSnapshot;

    @Column(name = "artifact_id", nullable = false, unique = true, length = 64)
    private String artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MoisArtifactStatus status;

    @Column(name = "offer_name", nullable = false, length = 255)
    private String offerName;

    @Column(name = "seller_or_brand", length = 255)
    private String sellerOrBrand;

    @Column(name = "channel", length = 64)
    private String channel;

    @Column(name = "target_audience_hypothesis", columnDefinition = "LONGTEXT")
    private String targetAudienceHypothesis;

    @Column(name = "core_promise", nullable = false, columnDefinition = "LONGTEXT")
    private String corePromise;

    @Column(name = "primary_offer_type", length = 128)
    private String primaryOfferType;

    @Column(name = "main_price", length = 128)
    private String mainPrice;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "deliverables_json", nullable = false, columnDefinition = "LONGTEXT")
    private String deliverablesJson;

    @Column(name = "price_points_json", nullable = false, columnDefinition = "LONGTEXT")
    private String pricePointsJson;

    @Column(name = "proof_summary", columnDefinition = "LONGTEXT")
    private String proofSummary;

    @Column(name = "mechanism_claim_summary", columnDefinition = "LONGTEXT")
    private String mechanismClaimSummary;

    @Column(name = "positioning_summary", columnDefinition = "LONGTEXT")
    private String positioningSummary;

    @Column(name = "funnel_pattern_summary", columnDefinition = "LONGTEXT")
    private String funnelPatternSummary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
