package com.marketinghub.experiment;

import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import jakarta.persistence.*;
import lombok.*;

/** Representa um item oficial da Meta usado em uma variação de público. */
@Entity
@Table(name = "experiment_audience_test_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAudienceTestItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "audience_test_id", nullable = false)
  @ToString.Exclude
  private ExperimentAudienceTest audienceTest;

  @Enumerated(EnumType.STRING)
  @Column(name = "candidate_type", nullable = false, length = 32)
  private TargetingCandidateType candidateType;

  @Column(name = "term", nullable = false, length = 191)
  private String term;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "targeting_element_id", nullable = false)
  @ToString.Exclude
  private TargetingElement targetingElement;
}
