package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.ExperimentAudienceTestStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** Dados de uma variação de público planejada para teste no experimento. */
@Builder
public record ExperimentAudienceTestDto(
    Long id,
    Long experimentId,
    String name,
    String hypothesis,
    String successMetric,
    BigDecimal dailyBudget,
    ExperimentAudienceTestStatus status,
    Long audienceSizeLowerBound,
    Long audienceSizeUpperBound,
    Instant createdAt,
    Instant updatedAt,
    List<Item> items) {
  /** Item oficial da Meta vinculado à variação de público. */
  @Builder
  public record Item(
      Long id,
      TargetingCandidateType candidateType,
      String term,
      Long targetingElementId,
      String metaId,
      String metaKey,
      Long metaAudienceSizeLowerBound,
      Long metaAudienceSizeUpperBound) {}
}
