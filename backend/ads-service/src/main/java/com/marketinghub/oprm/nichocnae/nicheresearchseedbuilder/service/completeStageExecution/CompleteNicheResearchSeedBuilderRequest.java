package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution;

import java.util.List;

/** Contrato de entrada para gravar o seed e as queries gerados pela IA na etapa dois. */
public record CompleteNicheResearchSeedBuilderRequest(
    String nicheName,
    String businessType,
    String operationType,
    String customerType,
    String commercialObjects,
    String initialAssumptions,
    String confidenceLevel,
    String createdBy,
    List<NicheResearchQueryRequest> queries) {}
