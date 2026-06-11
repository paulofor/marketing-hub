package com.marketinghub.oprm.nichocnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/**
 * Entidade responsável por guardar uma frase de pesquisa executável da pesquisa de rotina de nicho CNAE.
 */
@Entity
@Data
@Table(name = "oprm_research_query")
public class OprmResearchQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "research_cycle_id", nullable = false)
    private Long researchCycleId;

    @Column(name = "niche_research_seed_id", nullable = false)
    private Long nicheResearchSeedId;

    @Column(name = "query_text", columnDefinition = "LONGTEXT", nullable = false)
    private String queryText;

    @Column(name = "query_goal", columnDefinition = "LONGTEXT", nullable = false)
    private String queryGoal;

    @Column(name = "source_group", columnDefinition = "LONGTEXT")
    private String sourceGroup;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "result_count", nullable = false)
    private Integer resultCount;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_by", columnDefinition = "LONGTEXT", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
