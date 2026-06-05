package com.marketinghub.niche;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Entidade responsável por guardar o perfil enriquecido de um nicho materializado pelo OPRM. */
@Entity
@Data
@Table(name = "market_niche_enrichment_profile")
public class MarketNicheEnrichmentProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "market_niche_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private MarketNiche marketNiche;

  @Column(name = "source_module", nullable = false, length = 32)
  private String sourceModule;

  @Column(name = "source_niche_candidate_id")
  private Long sourceNicheCandidateId;

  @Column(name = "research_cycle_id", nullable = false)
  private Long researchCycleId;

  @Column(name = "routine_card_id", nullable = false)
  private Long sourceRoutineCardId;

  @Column(name = "cnae_code", nullable = false, length = 7)
  private String cnaeCode;

  @Column(name = "cnae_description", nullable = false, length = 255)
  private String cnaeDescription;

  @Column(name = "source_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal sourceScore;

  @Column(name = "quality_status", nullable = false, length = 32)
  private String qualityStatus;

  @Column(name = "specificity_score")
  private Integer specificityScore;

  @Column(name = "confidence_score")
  private Integer confidenceScore;

  @Column(name = "duplication_score")
  private Integer duplicationScore;

  @Column(name = "routine_summary", nullable = false, columnDefinition = "LONGTEXT")
  private String routineSummary;

  @Column(name = "pains_summary", nullable = false, columnDefinition = "LONGTEXT")
  private String painsSummary;

  @Column(name = "results_summary", nullable = false, columnDefinition = "LONGTEXT")
  private String resultsSummary;

  @Column(name = "mechanism_opportunities_summary", nullable = false, columnDefinition = "LONGTEXT")
  private String mechanismOpportunitiesSummary;

  @Column(name = "evidence_summary", nullable = false, columnDefinition = "LONGTEXT")
  private String evidenceSummary;

  @Column(name = "source_domains", length = 1000)
  private String sourceDomains;

  @Column(name = "persona_summary", columnDefinition = "LONGTEXT")
  private String personaSummary;

  @Column(name = "language_patterns", columnDefinition = "LONGTEXT")
  private String languagePatterns;

  @Column(name = "commercial_triggers", columnDefinition = "LONGTEXT")
  private String commercialTriggers;

  @Column(name = "objections", columnDefinition = "LONGTEXT")
  private String objections;

  @Column(name = "created_by", nullable = false, length = 64)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
