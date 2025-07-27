package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import com.marketinghub.niche.MarketNiche;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Experiment grouping ad sets and creatives.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"niche_id", "name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "niche_id", nullable = false)
    private MarketNiche niche;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String hypothesis;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id", nullable = false)
    private com.marketinghub.hypothesis.Hypothesis hypothesisRef;

    private java.math.BigDecimal kpiTarget;
    /** Stop-loss operacional em CPL. */
    @Column(precision = 7, scale = 2)
    private java.math.BigDecimal stopLossCpl;

    /** Tamanho de amostra desejado para o experimento. */
    private Integer sampleSize;

    /** MDE (Minimum Detectable Effect) percentual. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal mde;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ExperimentStatus status;

    @Enumerated(EnumType.STRING)
    private ExperimentPlatform platform;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
