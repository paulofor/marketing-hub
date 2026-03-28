package com.marketinghub.experiment.learning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketinghub.experiment.ExperimentStage;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estrutura esperada pela API quando o worker conclui a leitura do experimento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private Map<String, Object> openAiRequestPayload;
}
