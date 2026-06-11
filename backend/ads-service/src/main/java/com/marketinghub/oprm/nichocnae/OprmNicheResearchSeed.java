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
 * Entidade responsável por guardar o perfil operacional do nicho identificado para uma pesquisa CNAE.
 */
@Entity
@Data
@Table(name = "oprm_niche_research_seed")
public class OprmNicheResearchSeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "research_cycle_id", nullable = false)
    private Long researchCycleId;

    @Column(name = "cnae_code", nullable = false, length = 7)
    private String cnaeCode;

    @Column(name = "cnae_description", nullable = false, length = 255)
    private String cnaeDescription;

    @Column(name = "niche_name", columnDefinition = "LONGTEXT", nullable = false)
    private String nicheName;

    @Column(name = "business_type", columnDefinition = "LONGTEXT", nullable = false)
    private String businessType;

    @Column(name = "operation_type", columnDefinition = "LONGTEXT", nullable = false)
    private String operationType;

    @Column(name = "customer_type", columnDefinition = "LONGTEXT", nullable = false)
    private String customerType;

    @Column(name = "commercial_objects", columnDefinition = "LONGTEXT", nullable = false)
    private String commercialObjects;

    @Column(name = "initial_assumptions", columnDefinition = "LONGTEXT", nullable = false)
    private String initialAssumptions;

    @Column(name = "confidence_level", columnDefinition = "LONGTEXT", nullable = false)
    private String confidenceLevel;

    @Column(name = "created_by", columnDefinition = "LONGTEXT", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
