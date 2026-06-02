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
 * Entidade responsável por guardar uma fonte candidata encontrada pela etapa de busca do pipeline OPRM nicho CNAE.
 */
@Entity
@Data
@Table(name = "oprm_source_candidate")
public class OprmSourceCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "research_cycle_id", nullable = false)
    private Long researchCycleId;

    @Column(name = "research_query_id", nullable = false)
    private Long researchQueryId;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_title", nullable = false, length = 500)
    private String sourceTitle;

    @Column(name = "source_snippet", columnDefinition = "LONGTEXT")
    private String sourceSnippet;

    @Column(name = "source_domain", nullable = false, length = 255)
    private String sourceDomain;

    @Column(name = "source_group", nullable = false, length = 64)
    private String sourceGroup;

    @Column(name = "search_provider", nullable = false, length = 64)
    private String searchProvider;

    @Column(name = "search_position", nullable = false)
    private Integer searchPosition;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
