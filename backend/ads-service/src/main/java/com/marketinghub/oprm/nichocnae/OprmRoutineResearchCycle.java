package com.marketinghub.oprm.nichocnae;

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
 * Entidade responsável por controlar uma execução completa da pesquisa de rotina de um nicho CNAE.
 */
@Entity
@Data
@Table(name = "oprm_routine_research_cycle")
public class OprmRoutineResearchCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_niche_id", nullable = false)
    private Long sourceNicheId;

    @Column(name = "cnae_code", nullable = false, length = 7)
    private String cnaeCode;

    @Column(name = "cnae_description", nullable = false, length = 255)
    private String cnaeDescription;

    @Column(name = "niche_name", nullable = false, length = 255)
    private String nicheName;

    @Column(name = "source_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal sourceScore;

    @Column(name = "trigger_source", nullable = false, length = 32)
    private String triggerSource;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "total_queries", nullable = false)
    private Integer totalQueries;

    @Column(name = "total_source_candidates", nullable = false)
    private Integer totalSourceCandidates;

    @Column(name = "total_source_snapshots", nullable = false)
    private Integer totalSourceSnapshots;

    @Column(name = "total_extracted_signals", nullable = false)
    private Integer totalExtractedSignals;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
