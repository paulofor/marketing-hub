package com.marketinghub.pde.transitionpause.v1;

import java.util.List;

/** Define o contrato público e versionado do experimento Pausa de Transição. */
public record TransitionPauseContractResponse(
        String productSlug,
        String experienceVersion,
        String experimentName,
        String hypothesis,
        String primaryMetric,
        List<String> secondaryMetrics,
        List<String> allowedUse,
        List<String> forbiddenUse,
        List<String> stopSignals,
        List<VariantResponse> variants) {

    /** Descreve uma variante controlada do experimento. */
    public record VariantResponse(String code, String name, int durationSeconds, List<String> steps) {}
}
