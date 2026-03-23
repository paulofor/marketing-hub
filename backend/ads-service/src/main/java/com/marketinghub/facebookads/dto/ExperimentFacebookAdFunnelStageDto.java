package com.marketinghub.facebookads.dto;

import com.marketinghub.experiment.funnel.ExperimentFunnelStage;

/**
 * Representa a quantidade consolidada de conversões de um anúncio em uma etapa específica do funil.
 */
public record ExperimentFacebookAdFunnelStageDto(
        ExperimentFunnelStage stage,
        String label,
        int order,
        long totalCount
) {
}
