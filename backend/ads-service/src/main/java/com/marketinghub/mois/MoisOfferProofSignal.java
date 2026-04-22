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
@Table(name = "mois_offer_proof_signal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoisOfferProofSignal {

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

    @Column(name = "proof_type", length = 64)
    private String proofType;

    @Column(name = "proof_text", nullable = false, columnDefinition = "LONGTEXT")
    private String proofText;

    @Column(name = "proof_strength_hypothesis", length = 64)
    private String proofStrengthHypothesis;

    @Column(name = "proof_location", length = 128)
    private String proofLocation;

    @Column(name = "confidence")
    private Double confidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
