package com.marketinghub.oprm.cnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/**
 * Entidade responsável por armazenar o score de oportunidade de um CNAE calculado pelo módulo OPRM.
 */
@Entity
@Data
@Table(name = "oprm_cnae_opportunity_score")
public class OprmCnaeOpportunityScore {
    @Id
    @Column(name = "cnae_code", length = 7)
    private String cnaeCode;

    @Column(name = "cnae_description", nullable = false, length = 255)
    private String cnaeDescription;

    @Column(name = "opportunity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal opportunityScore;

    @Column(name = "market_volume_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal marketVolumeScore;

    @Column(name = "mei_density_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal meiDensityScore;

    @Column(name = "digital_fit_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal digitalFitScore;

    @Column(name = "pain_clarity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal painClarityScore;

    @Column(name = "score_justification", columnDefinition = "LONGTEXT")
    private String scoreJustification;

    @Column(name = "algorithm_version", nullable = false, length = 64)
    private String algorithmVersion;

    @Column(name = "cycle_id", nullable = false, length = 64)
    private String cycleId;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @Column(name = "score_status", nullable = false, length = 32)
    private String scoreStatus;

    @Column(name = "enriched_at")
    private Instant enrichedAt;
}
