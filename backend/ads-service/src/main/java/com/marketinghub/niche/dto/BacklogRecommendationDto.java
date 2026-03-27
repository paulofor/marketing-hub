package com.marketinghub.niche.dto;

import com.marketinghub.experiment.ExperimentStage;
import java.time.Instant;
import lombok.Data;

/**
 * Próximo teste sugerido a partir dos aprendizados já gerados para o nicho.
 */
@Data
public class BacklogRecommendationDto {
    private String title;
    private String rationale;
    private ExperimentStage stage;
    private String primaryMetric;
    private String priority;
    private Long experimentId;
    private String experimentName;
    private Instant completedAt;
}
