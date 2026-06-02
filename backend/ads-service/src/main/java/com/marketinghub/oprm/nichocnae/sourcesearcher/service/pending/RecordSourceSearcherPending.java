package com.marketinghub.oprm.nichocnae.sourcesearcher.service.pending;

import java.time.Instant;

/** Representa uma frase de pesquisa pendente para execução da etapa três de busca de fontes. */
public record RecordSourceSearcherPending(
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
