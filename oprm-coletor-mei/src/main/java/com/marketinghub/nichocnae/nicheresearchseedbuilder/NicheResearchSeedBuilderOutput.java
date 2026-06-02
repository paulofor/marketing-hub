package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.List;

/** Agrupa o seed operacional do nicho e as queries geradas pela IA na etapa dois. */
public record NicheResearchSeedBuilderOutput(
        Long researchCycleId,
        NicheResearchSeed seed,
        List<ResearchQuery> queries) {}
