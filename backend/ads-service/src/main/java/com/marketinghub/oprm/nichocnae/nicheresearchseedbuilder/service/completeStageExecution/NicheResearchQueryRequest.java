package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution;

/** Contrato de entrada de uma frase de pesquisa gerada para a etapa dois. */
public record NicheResearchQueryRequest(String queryText, String queryGoal, String sourceGroup, Integer priority) {}
