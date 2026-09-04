package com.marketinghub.researchintelligence.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Persiste conteúdo, fonte, estado e auditoria de uma versão imutável do cartão. */
@Entity
@Table(name = "research_intelligence_card_version")
@Getter
@NoArgsConstructor
public class ResearchIntelligenceCardVersion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "card_key", nullable = false, length = 120)
  private String cardKey;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "card_id", nullable = false, unique = true, length = 32)
  private String cardId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private ResearchIntelligenceCardStatus status;

  @Column(name = "collection_key", nullable = false, length = 80)
  private String collection;

  @Column(name = "title", nullable = false, length = 240)
  private String title;

  @Column(name = "finding", nullable = false, columnDefinition = "TEXT")
  private String finding;

  @Column(name = "mechanism", nullable = false, columnDefinition = "TEXT")
  private String mechanism;

  @Column(name = "commercial_application", nullable = false, columnDefinition = "TEXT")
  private String commercialApplication;

  @Column(name = "evidence_strength", nullable = false, length = 500)
  private String evidenceStrength;

  @Column(name = "published_on", nullable = false)
  private LocalDate publishedOn;

  @Column(name = "valid_until", nullable = false)
  private LocalDate validUntil;

  @Column(name = "experiment_hypothesis", nullable = false, columnDefinition = "TEXT")
  private String experimentHypothesis;

  @Column(name = "risks", nullable = false, columnDefinition = "TEXT")
  private String risks;

  @Column(name = "limits_text", nullable = false, columnDefinition = "TEXT")
  private String limits;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_kind", nullable = false, length = 24)
  private ResearchIntelligenceSourceKind sourceKind;

  @Column(name = "source_uri", nullable = false, length = 1024)
  private String sourceUri;

  @Column(name = "source_title", nullable = false, length = 240)
  private String sourceTitle;

  @Column(name = "source_sha256", nullable = false, length = 64)
  private String sourceSha256;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
  private String idempotencyKey;

  @Column(name = "payload_sha256", nullable = false, length = 64)
  private String payloadSha256;

  @Column(name = "created_by", nullable = false, length = 120)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "review_submitted_by", length = 120)
  private String reviewSubmittedBy;

  @Column(name = "review_submitted_at")
  private LocalDateTime reviewSubmittedAt;

  @Column(name = "review_note", length = 500)
  private String reviewNote;

  @Column(name = "activated_by", length = 120)
  private String activatedBy;

  @Column(name = "activated_at")
  private LocalDateTime activatedAt;

  @Column(name = "activation_note", length = 500)
  private String activationNote;

  @Column(name = "archived_by", length = 120)
  private String archivedBy;

  @Column(name = "archived_at")
  private LocalDateTime archivedAt;

  @Column(name = "archive_note", length = 500)
  private String archiveNote;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  /** Constrói uma versão em rascunho com sua fonte e auditoria completas. */
  public ResearchIntelligenceCardVersion(
      String cardKey,
      Integer versionNumber,
      String cardId,
      String collection,
      String title,
      String finding,
      String mechanism,
      String commercialApplication,
      String evidenceStrength,
      LocalDate publishedOn,
      LocalDate validUntil,
      String experimentHypothesis,
      String risks,
      String limits,
      ResearchIntelligenceSourceKind sourceKind,
      String sourceUri,
      String sourceTitle,
      String sourceSha256,
      String idempotencyKey,
      String payloadSha256,
      String createdBy,
      LocalDateTime now) {
    this.cardKey = cardKey;
    this.versionNumber = versionNumber;
    this.cardId = cardId;
    this.status = ResearchIntelligenceCardStatus.DRAFT;
    this.collection = collection;
    this.title = title;
    this.finding = finding;
    this.mechanism = mechanism;
    this.commercialApplication = commercialApplication;
    this.evidenceStrength = evidenceStrength;
    this.publishedOn = publishedOn;
    this.validUntil = validUntil;
    this.experimentHypothesis = experimentHypothesis;
    this.risks = risks;
    this.limits = limits;
    this.sourceKind = sourceKind;
    this.sourceUri = sourceUri;
    this.sourceTitle = sourceTitle;
    this.sourceSha256 = sourceSha256;
    this.idempotencyKey = idempotencyKey;
    this.payloadSha256 = payloadSha256;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
    this.rowVersion = 0L;
  }

  /** Encaminha um rascunho à revisão humana e registra sua justificativa. */
  public void submitForReview(String actor, String note, LocalDateTime now) {
    this.status = ResearchIntelligenceCardStatus.IN_REVIEW;
    this.reviewSubmittedBy = actor;
    this.reviewSubmittedAt = now;
    this.reviewNote = note;
    this.updatedAt = now;
  }

  /** Ativa a versão revisada sem alterar seu conteúdo editorial. */
  public void activate(String actor, String note, LocalDateTime now) {
    this.status = ResearchIntelligenceCardStatus.ACTIVE;
    this.activatedBy = actor;
    this.activatedAt = now;
    this.activationNote = note;
    this.updatedAt = now;
  }

  /** Arquiva a versão e preserva a causa e o ator da decisão. */
  public void archive(String actor, String note, LocalDateTime now) {
    this.status = ResearchIntelligenceCardStatus.ARCHIVED;
    this.archivedBy = actor;
    this.archivedAt = now;
    this.archiveNote = note;
    this.updatedAt = now;
  }
}
