package com.marketinghub.oprm.nichocnae.meiaudienceprofile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por persistir o perfil rastreável do público-alvo MEI/autônomo pesquisado pelo OPRM. */
@Entity
@Data
@Table(name = "oprm_mei_audience_profile")
public class OprmMeiAudienceProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "research_cycle_id", nullable = false)
  private Long researchCycleId;

  @Column(name = "routine_card_id")
  private Long routineCardId;

  @Column(name = "source_niche_candidate_id")
  private Long sourceNicheCandidateId;

  @Column(name = "market_niche_id")
  private Long marketNicheId;

  @Column(name = "cnae_code", nullable = false, length = 7)
  private String cnaeCode;

  @Column(name = "cnae_description", nullable = false, length = 255)
  private String cnaeDescription;

  @Column(name = "neutral_niche_name", nullable = false, length = 255)
  private String neutralNicheName;

  @Column(name = "audience_name", nullable = false, length = 255)
  private String audienceName;

  @Column(name = "occupation_terms", columnDefinition = "LONGTEXT")
  private String occupationTerms;

  @Column(name = "work_mode", columnDefinition = "LONGTEXT")
  private String workMode;

  @Column(name = "customer_acquisition_behavior", columnDefinition = "LONGTEXT")
  private String customerAcquisitionBehavior;

  @Column(name = "daily_routine_summary", columnDefinition = "LONGTEXT")
  private String dailyRoutineSummary;

  @Column(name = "recurring_tasks_summary", columnDefinition = "LONGTEXT")
  private String recurringTasksSummary;

  @Column(name = "operational_pains_summary", columnDefinition = "LONGTEXT")
  private String operationalPainsSummary;

  @Column(name = "emotional_pains_summary", columnDefinition = "LONGTEXT")
  private String emotionalPainsSummary;

  @Column(name = "dreams_summary", columnDefinition = "LONGTEXT")
  private String dreamsSummary;

  @Column(name = "fears_summary", columnDefinition = "LONGTEXT")
  private String fearsSummary;

  @Column(name = "language_patterns", columnDefinition = "LONGTEXT")
  private String languagePatterns;

  @Column(name = "channels_used", columnDefinition = "LONGTEXT")
  private String channelsUsed;

  @Column(name = "recent_source_summary", columnDefinition = "LONGTEXT")
  private String recentSourceSummary;

  @Column(name = "autonomous_professional_fit_score", nullable = false)
  private Integer autonomousProfessionalFitScore = 0;

  @Column(name = "behavioral_evidence_score", nullable = false)
  private Integer behavioralEvidenceScore = 0;

  @Column(name = "source_freshness_score", nullable = false)
  private Integer sourceFreshnessScore = 0;

  @Column(name = "outdated_source_risk_score", nullable = false)
  private Integer outdatedSourceRiskScore = 0;

  @Column(name = "structured_business_drift_risk_score", nullable = false)
  private Integer structuredBusinessDriftRiskScore = 0;

  @Column(name = "solution_language_risk_score", nullable = false)
  private Integer solutionLanguageRiskScore = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
