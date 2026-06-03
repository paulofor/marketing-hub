package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.time.Instant;

/** Representa uma query persistida e retornada pelo backend na conclusão da etapa dois. */
public record NicheResearchSeedBuilderBackendQueryResponse(
        Long queryId,
        Long researchCycleId,
        Long nicheResearchSeedId,
        String queryText,
        String queryGoal,
        String sourceGroup,
        Integer priority,
        String status,
        Integer resultCount,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {}
