package com.marketinghub.experiment.learning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.learning.LearningInsightType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Insight categorizado pelo framework oficial para compor o dicionário do nicho.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LearningInsightDto {
    private LearningInsightType type;
    private String statement;
    private String evidence;
    private String confidence;
    private ExperimentStage stage;
    private String primaryMetric;
}
