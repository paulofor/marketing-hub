package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution;

import java.time.Instant;

/** Representa uma frase de pesquisa persistida para execução pelas próximas etapas do pipeline. */
public record NicheResearchQueryResponse(
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
