package com.marketinghub.facebookads.service.recommendation;

import java.time.Instant;

/**
 * Contrato de saída das sugestões Meta salvas para uma campanha Facebook Ads.
 */
public record FacebookCampaignRecommendationDto(
        Long id,
        String campaignId,
        String recommendationCode,
        String title,
        String message,
        String importance,
        String confidence,
        String blameField,
        String recommendationDataJson,
        String rawJson,
        Instant collectedAt) {}
