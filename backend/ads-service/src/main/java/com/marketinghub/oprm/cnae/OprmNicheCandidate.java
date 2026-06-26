package com.marketinghub.oprm.cnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/**
 * Entidade responsável por armazenar candidatos de nicho derivados de CNAEs enriquecidos pelo OPRM.
 */
@Entity
@Data
@Table(name = "oprm_niche_candidate")
public class OprmNicheCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cnae_code", nullable = false, length = 7)
    private String cnaeCode;

    @Column(name = "cnae_description", nullable = false, length = 255)
    private String cnaeDescription;

    @Column(name = "candidate_niche_name", nullable = false, length = 255)
    private String candidateNicheName;

    @Column(name = "persona", columnDefinition = "LONGTEXT")
    private String persona;

    @Column(name = "pain_hypothesis", columnDefinition = "LONGTEXT")
    private String painHypothesis;

    @Column(name = "desired_outcome", columnDefinition = "LONGTEXT")
    private String desiredOutcome;

    @Column(name = "mechanism_hypothesis", columnDefinition = "LONGTEXT")
    private String mechanismHypothesis;

    @Column(name = "proof_direction", columnDefinition = "LONGTEXT")
    private String proofDirection;

    @Column(name = "offer_idea", columnDefinition = "LONGTEXT")
    private String offerIdea;

    @Column(name = "market_volume_signals", columnDefinition = "LONGTEXT")
    private String marketVolumeSignals;

    @Column(name = "opportunity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal opportunityScore;

    @Column(name = "score_cycle_id", nullable = false, length = 64)
    private String scoreCycleId;

    @Column(name = "enrichment_cycle_id", nullable = false, length = 64)
    private String enrichmentCycleId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "source_artifacts", columnDefinition = "LONGTEXT")
    private String sourceArtifacts;

    @Column(name = "market_niche_id")
    private Long marketNicheId;

    @Column(name = "routine_research_status", nullable = false, length = 32)
    private String routineResearchStatus;

    @Column(name = "geracao_anuncios_pipeline_status", length = 32)
    private String geracaoAnunciosPipelineStatus;

    @Column(name = "geracao_anuncios_current_stage_code", length = 64)
    private String geracaoAnunciosCurrentStageCode;

    @Column(name = "last_routine_research_cycle_id")
    private Long lastRoutineResearchCycleId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
