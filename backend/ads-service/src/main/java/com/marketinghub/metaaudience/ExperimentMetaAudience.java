package com.marketinghub.metaaudience;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Representa o uso planejado de uma audiência CNAE em um experimento comercial. */
@Entity
@Table(
        name = "experiment_meta_audience",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_experiment_meta_audience",
                columnNames = {"experiment_id", "meta_audience_id", "meta_audience_segment_id"}))
@Getter
@Setter
public class ExperimentMetaAudience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_audience_id", nullable = false)
    private MetaAudience metaAudience;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_audience_segment_id", nullable = false)
    private MetaAudienceSegment metaAudienceSegment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id", nullable = false)
    private MarketNiche marketNiche;

    @Column(name = "activation_status", nullable = false, length = 32)
    private String activationStatus;

    @Column(name = "channel", length = 64)
    private String channel;

    @Lob
    @Column(name = "pain_angle")
    private String painAngle;

    @Lob
    @Column(name = "promise")
    private String promise;

    @Lob
    @Column(name = "offer")
    private String offer;

    @Lob
    @Column(name = "decision_snapshot_json")
    private String decisionSnapshotJson;

    @Lob
    @Column(name = "analysis_summary")
    private String analysisSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
