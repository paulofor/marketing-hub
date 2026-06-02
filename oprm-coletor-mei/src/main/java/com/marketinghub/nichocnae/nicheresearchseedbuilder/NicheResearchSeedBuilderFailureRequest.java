package com.marketinghub.nichocnae.nicheresearchseedbuilder;

/** Comunica ao backend uma falha da etapa dois com contexto suficiente para diagnóstico operacional. */
public record NicheResearchSeedBuilderFailureRequest(
        Long researchCycleId,
        String stageCode,
        String errorMessage,
        String errorDetail) {}
