package com.marketinghub.creative.service.videoreview;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carrega somente classificação e linhagem, sem mídia, prompts ou pareceres integrais. */
public record CreativeVideoSummarySnapshot(
    Long id,
    Long experimentId,
    ExperimentStatus experimentStatus,
    Long sourceId,
    CreativeStatus status,
    CreativeAgentReviewStatus agentReviewStatus,
    String primaryText,
    String headline,
    String description,
    boolean eligible) {

  /** Reconstrói apenas as relações necessárias à mesma política usada pela fila detalhada. */
  public static List<Creative> eligibleCreatives(List<CreativeVideoSummarySnapshot> rows) {
    Map<Long, Creative> lineage = new LinkedHashMap<>();
    for (var row : rows) {
      lineage.put(
          row.id(),
          Creative.builder()
              .id(row.id())
              .experiment(
                  Experiment.builder()
                      .id(row.experimentId())
                      .status(row.experimentStatus())
                      .build())
              .status(row.status())
              .agentReviewStatus(row.agentReviewStatus())
              .primaryText(row.primaryText())
              .headline(row.headline())
              .description(row.description())
              .build());
    }
    for (var row : rows) {
      lineage.get(row.id()).setSourceCreative(lineage.get(row.sourceId()));
    }
    return rows.stream()
        .filter(CreativeVideoSummarySnapshot::eligible)
        .map(row -> lineage.get(row.id()))
        .toList();
  }
}
