package com.marketinghub.hypothesis.pain.service.pending;

/** Dados do nicho usados pelo Worker AI para construir a dor. */
public record HypothesisPainPendingNiche(
        Long id,
        String name,
        String description,
        String demandVolume,
        String promises,
        String offers,
        String baseSegmentation,
        String interests,
        String demographicFilters,
        String extraTips
) {
}
