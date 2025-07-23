package com.marketinghub.hypothesis;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.creative.label.Angle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class Hypothesis {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(nullable = false)
    private String title;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "premise_angle_id", nullable = false)
    private Angle premiseAngle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferType offerType;

    @Column(precision = 6, scale = 2)
    private BigDecimal price;
    @Column(precision = 7, scale = 2, nullable = false)
    private BigDecimal kpiTargetCpl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private HypothesisStatus status = HypothesisStatus.BACKLOG;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
