package com.marketinghub.experiment.learning.dto;

import com.marketinghub.experiment.ExperimentStage;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Estrutura esperada pela API quando o worker conclui a leitura do experimento.
 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ExperimentLearningPayloadDto {
    private ExperimentStage stage;
    private String primaryMetric;
    private String metricSignal;
    private String summary;
    private String whatWorked;
    private String whatBlocked;
    private String nextTest;
    private List<LearningInsightDto> insights;
    private List<ExperimentLearningSuggestionDto> suggestions;
}
