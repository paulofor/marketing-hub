package com.marketinghub.oprm.generalaudience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** Responsável por armazenar evidência agregada e rastreável usada no mapeamento de público geral. */
@Entity
@Table(name = "oprm_general_audience_source_evidence")
public class OprmGeneralAudienceSourceEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seed_id", nullable = false)
    private OprmGeneralAudienceSeed seed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subniche_id")
    private OprmGeneralAudienceSubniche subniche;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "source_domain", length = 191)
    private String sourceDomain;

    @Column(name = "source_type", length = 64)
    private String sourceType;

    @Column(name = "evidence_summary", nullable = false, columnDefinition = "LONGTEXT")
    private String evidenceSummary;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    /** Define a data de captura quando a origem não informou uma data explícita. */
    @PrePersist
    void onCreate() {
        if (capturedAt == null) {
            capturedAt = Instant.now();
        }
    }

    /** Retorna o identificador da evidência. */
    public Long getId() { return id; }

    /** Define o identificador da evidência. */
    public void setId(Long id) { this.id = id; }

    /** Retorna a semente associada à evidência. */
    public OprmGeneralAudienceSeed getSeed() { return seed; }

    /** Define a semente associada à evidência. */
    public void setSeed(OprmGeneralAudienceSeed seed) { this.seed = seed; }

    /** Retorna o subnicho associado à evidência, quando existir. */
    public OprmGeneralAudienceSubniche getSubniche() { return subniche; }

    /** Define o subnicho associado à evidência, quando existir. */
    public void setSubniche(OprmGeneralAudienceSubniche subniche) { this.subniche = subniche; }

    /** Retorna a URL rastreável da fonte. */
    public String getSourceUrl() { return sourceUrl; }

    /** Define a URL rastreável da fonte. */
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    /** Retorna o domínio da fonte. */
    public String getSourceDomain() { return sourceDomain; }

    /** Define o domínio da fonte. */
    public void setSourceDomain(String sourceDomain) { this.sourceDomain = sourceDomain; }

    /** Retorna o tipo operacional da fonte. */
    public String getSourceType() { return sourceType; }

    /** Define o tipo operacional da fonte. */
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    /** Retorna o resumo agregado de evidência sem dados pessoais. */
    public String getEvidenceSummary() { return evidenceSummary; }

    /** Define o resumo agregado de evidência sem dados pessoais. */
    public void setEvidenceSummary(String evidenceSummary) { this.evidenceSummary = evidenceSummary; }

    /** Retorna a data em que a evidência foi capturada. */
    public Instant getCapturedAt() { return capturedAt; }

    /** Define a data em que a evidência foi capturada. */
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
}
