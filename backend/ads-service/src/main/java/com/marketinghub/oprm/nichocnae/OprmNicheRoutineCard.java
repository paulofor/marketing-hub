package com.marketinghub.oprm.nichocnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por armazenar o cartão de rotina sintetizado para um nicho CNAE pesquisado. */
@Entity
@Data
@Table(name = "oprm_niche_routine_card")
public class OprmNicheRoutineCard {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "research_cycle_id", nullable = false)
  private Long researchCycleId;

  @Column(name = "niche_name", nullable = false, length = 255)
  private String nicheName;

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

  @Column(name = "source_domains", nullable = false, length = 1000)
  private String sourceDomains;

  @Column(name = "confidence_score", nullable = false)
  private Integer confidenceScore;

  @Column(name = "synthesized_by", nullable = false, length = 64)
  private String synthesizedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
