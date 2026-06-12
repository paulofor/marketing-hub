package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.time.Instant;
import java.util.List;

/** Representa a resposta achatada do backend após persistir o seed e as queries da etapa dois. */
public record NicheResearchSeedBuilderBackendCompletionResponse(
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
        List<NicheResearchSeedBuilderBackendQueryResponse> queries) {}
