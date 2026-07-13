package com.marketinghub.experiment.dto;

/**
 * Resume o tempo medio de sessao de uma variante A/B rastreada na landing.
 */
public record ExperimentSessionDurationVariantDto(
        String variantKey,
        String variantName,
        long sessions,
        long averageVisibleMsPerSession) {
}
