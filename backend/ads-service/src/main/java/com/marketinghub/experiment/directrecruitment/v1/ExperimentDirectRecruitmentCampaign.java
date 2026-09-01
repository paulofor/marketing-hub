package com.marketinghub.experiment.directrecruitment.v1;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: persistir o convite versionado que forma a amostra direta de um experimento.
 */
@Entity
@Table(
    name = "experiment_direct_recruitment_campaign",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_exp_direct_recruit_campaign_exp", columnNames = "experiment_id"),
      @UniqueConstraint(name = "uk_exp_direct_recruit_campaign_token", columnNames = "public_token")
    })
@Getter
@Setter
public class ExperimentDirectRecruitmentCampaign {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;

  @Column(name = "public_token", nullable = false, length = 36)
  private String publicToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private DirectRecruitmentCampaignStatus status;

  @Column(name = "contract_version", nullable = false, length = 32)
  private String contractVersion;

  @Column(name = "headline", nullable = false, length = 255)
  private String headline;

  @Column(name = "body_text", nullable = false, length = 1000)
  private String bodyText;

  @Column(name = "audience_summary", nullable = false, length = 1000)
  private String audienceSummary;

  @Column(name = "consent_text", nullable = false, length = 1500)
  private String consentText;

  @Column(name = "consent_version", nullable = false, length = 32)
  private String consentVersion;

  @Column(name = "offer_url", nullable = false, length = 1200)
  private String offerUrl;

  @Column(name = "offer_cta", nullable = false, length = 191)
  private String offerCta;

  @Column(name = "privacy_policy_url", nullable = false, length = 1200)
  private String privacyPolicyUrl;

  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @Column(name = "status_changed_by", length = 100)
  private String statusChangedBy;

  @Column(name = "status_reason", length = 500)
  private String statusReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "activated_at")
  private Instant activatedAt;

  @Column(name = "paused_at")
  private Instant pausedAt;

  @Column(name = "completed_at")
  private Instant completedAt;
}
