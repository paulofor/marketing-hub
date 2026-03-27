package com.marketinghub.experiment.learning.dto;

import com.marketinghub.experiment.ExperimentStage;
import lombok.Data;

/**
 * Sugestão estruturada de próximo teste para alimentar o backlog do nicho.
 */
@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ExperimentLearningSuggestionDto {
    private String title;
    private String rationale;
    private ExperimentStage stage;
    private String primaryMetric;
    private String priority;
}
