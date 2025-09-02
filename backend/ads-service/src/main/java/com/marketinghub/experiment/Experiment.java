package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import com.marketinghub.niche.MarketNiche;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.marketinghub.funnel.SalesFunnel;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_preset_id")
    private MetricPreset metricPreset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_funnel_id")
    private SalesFunnel salesFunnel;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal kpiTargetCpl;
    /** Stop-loss operacional em CPL. */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal stopLossCpl;

    /** Tamanho de amostra desejado para o experimento. */
    private Integer sampleSize;

    /** Taxa de conversão base para comparação. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal baselineCvr;

    /** Taxa de conversão desejada para sucesso. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal targetCvr;

    /** MDE (Minimum Detectable Effect) percentual. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ExperimentStatus status;

    @Enumerated(EnumType.STRING)
    private ExperimentPlatform platform;

    /** Quantidade de criativos a serem gerados pelo worker. */
    @Column(name = "creatives_to_generate")
    private Integer creativesToGenerate;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @PrePersist
    void applyMetricPreset() {
        if (metricPreset != null) {
            if (sampleSize == null) {
                sampleSize = metricPreset.getSampleSize();
            }
            if (stopLossCpl == null && kpiTargetCpl != null && metricPreset.getStopLossFactor() != null) {
                stopLossCpl = kpiTargetCpl.multiply(metricPreset.getStopLossFactor());
            }
            if (mdePercent == null) {
                mdePercent = metricPreset.getDefaultMdePp();
            }
        }
    }
}
