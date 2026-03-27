package com.marketinghub.experiment.learning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketinghub.experiment.ExperimentStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sugestão estruturada de próximo teste para alimentar o backlog do nicho.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentLearningSuggestionDto {
    private String title;
    private String rationale;
    private ExperimentStage stage;
    private String primaryMetric;
    private String priority;
}
