package com.marketinghub.proof;

import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Catalogued proof asset tied to a hypothesis or experiment.
 */
@Entity
@Table(name = "proof_artifact")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id")
    private MarketNiche marketNiche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private Hypothesis hypothesis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visual_proof_id")
    private VisualProof visualProof;

    @Enumerated(EnumType.STRING)
    private ProofStage stage;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProofStatus status = ProofStatus.DRAFT;

    @Column(name = "custom_type", length = 120)
    private String customType;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String assetPlan;

    @Column(name = "asset_url", length = 512)
    private String assetUrl;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String message;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String deliveryNotes;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @Column(length = 255)
    private String model;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
