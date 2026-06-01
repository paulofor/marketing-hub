package com.marketinghub.oprm.cnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/**
 * Entidade responsável por armazenar artefatos de enriquecimento de CNAE publicados pelo módulo OPRM.
 */
@Entity
@Data
@Table(name = "oprm_cnae_enrichment_artifact")
public class OprmCnaeEnrichmentArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cnae_code", nullable = false, length = 7)
    private String cnaeCode;

    @Column(name = "enrichment_cycle_id", nullable = false, length = 64)
    private String enrichmentCycleId;

    @Column(name = "routine_signals", columnDefinition = "LONGTEXT")
    private String routineSignals;

    @Column(name = "pain_signals", columnDefinition = "LONGTEXT")
    private String painSignals;

    @Column(name = "mechanism_signals", columnDefinition = "LONGTEXT")
    private String mechanismSignals;

    @Column(name = "proof_signals", columnDefinition = "LONGTEXT")
    private String proofSignals;

    @Column(name = "offer_signals", columnDefinition = "LONGTEXT")
    private String offerSignals;

    @Column(name = "source_summary", columnDefinition = "LONGTEXT")
    private String sourceSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
