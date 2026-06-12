package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution;

import java.time.Instant;
import java.util.List;

/** Informa o seed e as frases de pesquisa gravadas como saída validada da etapa dois. */
public record CompleteNicheResearchSeedBuilderResponse(
    Long researchCycleId,
    Long nicheResearchSeedId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    String businessType,
    String operationType,
    String customerType,
    String commercialObjects,
    String initialAssumptions,
    String confidenceLevel,
    String createdBy,
    Instant createdAt,
    String model,
    String rawModelResponse,
    Integer inputTokens,
    Integer outputTokens,
    java.math.BigDecimal costUsd,
    String openAiResponseId,
    Integer totalQueries,
    List<NicheResearchQueryResponse> queries) {}
