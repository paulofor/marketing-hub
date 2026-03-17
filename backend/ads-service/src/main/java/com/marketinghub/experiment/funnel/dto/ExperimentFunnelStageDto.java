package com.marketinghub.experiment.funnel.dto;

import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import lombok.Data;

import java.time.Instant;

/**
 * Resumo de uma etapa do funil de vendas do experimento.
 */
@Data
public class ExperimentFunnelStageDto {
    private ExperimentFunnelStage stage;
    private String label;
    private int order;
    private long autoCount;
    private long manualCount;
    private long totalCount;
    private Long uniqueCount;
    private Instant lastEventAt;
    private String source;
}
