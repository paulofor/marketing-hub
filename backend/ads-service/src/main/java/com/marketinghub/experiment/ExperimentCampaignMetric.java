package com.marketinghub.experiment;

import com.marketinghub.facebookads.FacebookAdsCampaign;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Guarda as métricas agregadas de desempenho da campanha Facebook vinculada ao experimento.
 */
@Entity
@Table(name = "experiment_campaign_metric")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentCampaignMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private FacebookAdsCampaign campaign;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false, unique = true)
    private Experiment experiment;

    private LocalDate dateStart;
    private LocalDate dateStop;
    @Column(name = "reach")
    private Long reach;
    private Long impressions;
    private Long clicks;
    private Long leads;

    @Column(precision = 12, scale = 2)
    private BigDecimal spend;

    @Column(precision = 12, scale = 2)
    private BigDecimal cpc;

    @Column(precision = 12, scale = 2)
    private BigDecimal cpl;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
