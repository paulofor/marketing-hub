package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.detailStageExecution;

import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderResponse;

/** Detalha o resultado persistido da etapa dois para consulta operacional no backend OPRM. */
public record NicheResearchSeedBuilderDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Integer cycleTotalQueries,
    String cycleErrorMessage,
    CompleteNicheResearchSeedBuilderResponse seed) {}
