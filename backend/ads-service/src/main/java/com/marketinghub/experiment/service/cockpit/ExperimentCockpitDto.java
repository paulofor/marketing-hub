package com.marketinghub.experiment.service.cockpit;

import java.util.List;

/** Contrato consolidado do cockpit comercial de um experimento. */
public record ExperimentCockpitDto(
    Long experimentId,
    String experimentName,
    String status,
    String experimentType,
    String campaignObjective,
    ExperimentCockpitScoreboardDto scoreboard,
    ExperimentCockpitQuestionDto question,
    ExperimentCockpitHealthDto health,
    List<ExperimentCockpitFunnelStageDto> funnel,
    ExperimentCockpitBottleneckDto bottleneck,
    List<String> learnings,
    List<ExperimentCockpitActionDto> nextActions) {}
