package com.marketinghub.hypothesis;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.creative.label.Angle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hypothesis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "premise_angle_id")
    private Angle premiseAngle;

    @Enumerated(EnumType.STRING)
    private OfferType offerType;

    private BigDecimal kpiTargetCpl;

    @Enumerated(EnumType.STRING)
    private HypothesisStatus status;

    @CreationTimestamp
    private Instant createdAt;
}
