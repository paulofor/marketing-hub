package com.marketinghub.oprm.nichocnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por guardar metadados e trechos curtos coletados de uma fonte selecionada. */
@Entity
@Data
@Table(name = "oprm_source_snapshot")
public class OprmSourceSnapshot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "research_cycle_id", nullable = false)
  private Long researchCycleId;

  @Column(name = "source_candidate_id", nullable = false)
  private Long sourceCandidateId;

  @Column(name = "source_url", nullable = false, length = 1000)
  private String sourceUrl;

  @Column(name = "source_domain", nullable = false, length = 255)
  private String sourceDomain;

  @Column(name = "source_title", nullable = false, length = 500)
  private String sourceTitle;

  @Column(name = "source_type", nullable = false, length = 64)
  private String sourceType;

  @Column(name = "source_intent", length = 64)
  private String sourceIntent;

  @Column(name = "routine_evidence_score")
  private Integer routineEvidenceScore;

  @Column(name = "commercial_page_risk", nullable = false)
  private Boolean commercialPageRisk;

  @Column(name = "solution_language_risk", nullable = false)
  private Boolean solutionLanguageRisk;

  @Column(name = "snippet", columnDefinition = "LONGTEXT")
  private String snippet;

  @Column(name = "short_excerpt", columnDefinition = "LONGTEXT")
  private String shortExcerpt;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  @Column(name = "fetch_status", nullable = false, length = 32)
  private String fetchStatus;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "storage_policy", nullable = false, length = 64)
  private String storagePolicy;

  @Column(name = "license_state", length = 64)
  private String licenseState;

  @Column(name = "error_message", columnDefinition = "LONGTEXT")
  private String errorMessage;

  @Column(name = "signal_extraction_status", nullable = false, length = 32)
  private String signalExtractionStatus;

  @Column(name = "signal_extraction_error", columnDefinition = "LONGTEXT")
  private String signalExtractionError;

  @Column(name = "signal_extracted_at")
  private Instant signalExtractedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
