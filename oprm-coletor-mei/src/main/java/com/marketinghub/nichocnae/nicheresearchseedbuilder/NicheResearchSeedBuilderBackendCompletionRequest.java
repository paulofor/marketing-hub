package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.List;

/** Contrato achatado enviado ao backend para concluir a etapa dois conforme o DTO canônico. */
public record NicheResearchSeedBuilderBackendCompletionRequest(
        String nicheName,
        String businessType,
        String operationType,
        String customerType,
        String commercialObjects,
        String initialAssumptions,
        String confidenceLevel,
        String createdBy,
        String model,
        String rawModelResponse,
        Integer inputTokens,
        Integer outputTokens,
        String openAiResponseId,
        List<ResearchQuery> queries) {}
