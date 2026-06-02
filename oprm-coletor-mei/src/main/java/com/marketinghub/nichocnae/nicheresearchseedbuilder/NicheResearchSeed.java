package com.marketinghub.nichocnae.nicheresearchseedbuilder;

/** Descreve o nicho operacional identificado pela etapa dois antes da execução das buscas. */
public record NicheResearchSeed(
        Long researchCycleId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        String businessType,
        String operationType,
        String customerType,
        String commercialObjects,
        String initialAssumptions,
        String confidenceLevel,
        String createdBy) {}
