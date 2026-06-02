package com.marketinghub.nichocnae.nicheresearchseedbuilder;

/** Representa uma frase objetiva que será pesquisada individualmente nas próximas etapas do pipeline. */
public record ResearchQuery(
        Long researchCycleId,
        String queryText,
        String queryGoal,
        String sourceGroup,
        Integer priority,
        String status,
        String createdBy) {}
