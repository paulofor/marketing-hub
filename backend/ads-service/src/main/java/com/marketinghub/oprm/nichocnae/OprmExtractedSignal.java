package com.marketinghub.oprm.nichocnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por armazenar sinais estruturados extraídos de snapshots públicos do pipeline OPRM NichoCNAE. */
@Entity
@Data
@Table(name = "oprm_extracted_signal")
public class OprmExtractedSignal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "research_cycle_id", nullable = false)
  private Long researchCycleId;

  @Column(name = "source_snapshot_id", nullable = false)
  private Long sourceSnapshotId;

  @Column(name = "source_candidate_id", nullable = false)
  private Long sourceCandidateId;

  @Column(name = "signal_type", nullable = false, length = 64)
  private String signalType;

  @Column(name = "signal_text", nullable = false, length = 500)
  private String signalText;

  @Column(name = "evidence_excerpt", nullable = false, length = 1000)
  private String evidenceExcerpt;

  @Column(name = "source_domain", nullable = false, length = 255)
  private String sourceDomain;

  @Column(name = "confidence_score", nullable = false)
  private Integer confidenceScore;

  @Column(name = "created_by", nullable = false, length = 64)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
