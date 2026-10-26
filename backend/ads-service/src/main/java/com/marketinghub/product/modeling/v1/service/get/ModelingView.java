package com.marketinghub.product.modeling.v1.service.get;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

/** Apresenta a intenção, a situação real da pesquisa e a navegação canônica da cadeia. */
public record ModelingView(
    Long id,
    Long productId,
    String productName,
    String desiredOutcome,
    List<String> referenceUrls,
    JsonNode bigIdeaSnapshot,
    Long discoveryCycleId,
    String status,
    String guidance,
    String researchUrl,
    String productUrl,
    Instant createdAt,
    List<ModelingStageView> stages,
    Long learningCycleId,
    String chainUrl,
    List<ModelingCycleOption> availableCycles,
    String researchScope,
    String targetMarket,
    JsonNode internationalAssessment,
    ModelingResearchRecoveryView researchRecovery) {}
