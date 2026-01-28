package com.marketinghub.facebookads;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Snapshot of Meta campaign metrics captured by the worker.
 */
@Entity
@Table(name = "facebook_campaign_metric_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class FacebookCampaignMetricSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, columnDefinition = "CHAR(36)")
    private FacebookAdsCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "account_id", length = 32)
    private String accountId;

    @Column(name = "currency", length = 16)
    private String currency;

    @Column(name = "date_start")
    private LocalDate dateStart;

    @Column(name = "date_stop")
    private LocalDate dateStop;

    private Long impressions;
    private Long reach;
    private Long clicks;

    @Column(precision = 19, scale = 4)
    private BigDecimal spend;

    @Column(precision = 19, scale = 4)
    private BigDecimal cpc;

    @Column(precision = 19, scale = 4)
    private BigDecimal cpm;

    @Column(precision = 19, scale = 4)
    private BigDecimal ctr;

    @Column(precision = 19, scale = 4)
    private BigDecimal frequency;

    private Integer leads;

    @Lob
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;
}
