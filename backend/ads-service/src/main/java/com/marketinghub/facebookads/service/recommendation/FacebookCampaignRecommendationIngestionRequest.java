package com.marketinghub.facebookads.service.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Contrato de entrada usado pelo worker para reportar sugestões coletadas na Meta.
 */
public record FacebookCampaignRecommendationIngestionRequest(
        Instant collectedAt,
        JsonNode recommendations) {}
