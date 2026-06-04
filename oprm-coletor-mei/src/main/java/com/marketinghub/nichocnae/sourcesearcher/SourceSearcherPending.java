package com.marketinghub.nichocnae.sourcesearcher;

import java.time.Instant;

/** Representa uma frase de pesquisa pendente para execução da etapa três no coletor OPRM. */
public record SourceSearcherPending(
        Long researchQueryId,
        Long researchCycleId,
        Long nicheResearchSeedId,
        String queryText,
        String queryGoal,
        String sourceGroup,
        Integer priority,
        String status,
        Integer resultCount,
        Instant createdAt,
        Instant updatedAt) {}
