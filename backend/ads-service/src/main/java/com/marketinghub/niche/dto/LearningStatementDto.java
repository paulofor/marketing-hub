package com.marketinghub.niche.dto;

import com.marketinghub.experiment.learning.LearningInsightType;
import java.time.Instant;
import lombok.Data;

/**
 * Entrada normalizada do dicionário de aprendizados do nicho.
 */
@Data
public class LearningStatementDto {
    private LearningInsightType type;
    private String statement;
    private String evidence;
    private String confidence;
    private Long experimentId;
    private String experimentName;
    private Instant completedAt;
    private String metricSignal;
}
