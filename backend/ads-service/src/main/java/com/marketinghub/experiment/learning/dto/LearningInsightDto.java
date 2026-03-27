package com.marketinghub.experiment.learning.dto;

import com.marketinghub.experiment.learning.LearningInsightType;
import com.marketinghub.experiment.ExperimentStage;
import lombok.Data;

/**
 * Insight categorizado pelo framework oficial para compor o dicionário do nicho.
 */
@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LearningInsightDto {
    private LearningInsightType type;
    private String statement;
    private String evidence;
    private String confidence;
    private ExperimentStage stage;
    private String primaryMetric;
}
