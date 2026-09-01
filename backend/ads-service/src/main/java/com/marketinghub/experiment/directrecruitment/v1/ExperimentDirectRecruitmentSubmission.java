package com.marketinghub.experiment.directrecruitment.v1;

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

/** Responsabilidade: persistir a qualificação e o consentimento de uma adesão inbound. */
@Entity
@Table(
    name = "experiment_direct_recruitment_submission",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_exp_direct_recruit_submission_key",
          columnNames = {"campaign_id", "submission_key"}),
      @UniqueConstraint(
          name = "uk_exp_direct_recruit_submission_contact",
          columnNames = {"campaign_id", "contact_fingerprint"})
    })
@Getter
@Setter
public class ExperimentDirectRecruitmentSubmission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private ExperimentDirectRecruitmentCampaign campaign;

  @Column(name = "submission_key", nullable = false, length = 36)
  private String submissionKey;

  @Column(name = "contact_fingerprint", nullable = false, length = 64)
  private String contactFingerprint;

  @Column(name = "service_segment", nullable = false, length = 48)
  private String serviceSegment;

  @Column(name = "weekly_conversations_range", nullable = false, length = 32)
  private String weeklyConversationsRange;

  @Column(name = "uses_whatsapp", nullable = false)
  private boolean usesWhatsapp;

  @Column(name = "decision_maker", nullable = false)
  private boolean decisionMaker;

  @Column(name = "wants_personalized_implementation", nullable = false)
  private boolean wantsPersonalizedImplementation;

  @Column(name = "consent_accepted", nullable = false)
  private boolean consentAccepted;

  @Column(name = "consent_version", nullable = false, length = 32)
  private String consentVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private DirectRecruitmentSubmissionStatus status;

  @Column(name = "qualification_reason", nullable = false, length = 500)
  private String qualificationReason;

  @Column(name = "utm_source", length = 100)
  private String utmSource;

  @Column(name = "utm_medium", length = 100)
  private String utmMedium;

  @Column(name = "utm_campaign", length = 100)
  private String utmCampaign;

  @Column(name = "utm_content", length = 100)
  private String utmContent;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
