package com.marketinghub.experiment.directrecruitment.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Responsabilidade: representar uma visita única e pseudonimizada ao convite público. */
@Entity
@Table(
    name = "experiment_direct_recruitment_visit",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_exp_direct_recruit_visit_visitor",
            columnNames = {"campaign_id", "visitor_fingerprint"}))
@Getter
@Setter
public class ExperimentDirectRecruitmentVisit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private ExperimentDirectRecruitmentCampaign campaign;

  @Column(name = "visitor_fingerprint", nullable = false, length = 64)
  private String visitorFingerprint;

  @Column(name = "utm_source", length = 100)
  private String utmSource;

  @Column(name = "utm_medium", length = 100)
  private String utmMedium;

  @Column(name = "utm_campaign", length = 100)
  private String utmCampaign;

  @Column(name = "utm_content", length = 100)
  private String utmContent;

  @Column(name = "first_visited_at", nullable = false)
  private Instant firstVisitedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
