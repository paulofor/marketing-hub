package com.marketinghub.experiment.learning.dto;

import com.marketinghub.experiment.ExperimentStage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/**
 * DTO exposto para o frontend com o resultado consolidado da análise do experimento.
 */
@Data
public class ExperimentLearningDto {
    private Long id;
    private Long experimentId;
    private Long requestId;
    private Long nicheId;
    private UUID hypothesisId;
    private ExperimentStage stage;
    private String primaryMetric;
    private String metricSignal;
    private String summary;
    private String whatWorked;
    private String whatBlocked;
    private String nextTest;
    private Instant completedAt;
    private Instant createdAt;
    private List<LearningInsightDto> insights;
    private List<ExperimentLearningSuggestionDto> suggestions;
}
