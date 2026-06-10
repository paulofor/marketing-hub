package com.marketinghub.facebookads.service.recommendation;

import java.time.Instant;

/**
 * Contrato de leitura de campanha ativa que precisa de coleta de sugestões na Meta.
 */
public record FacebookCampaignRecommendationSyncTarget(
        String campaignId,
        String externalCampaignId,
        Long experimentId,
        String adAccountId,
        Instant lastSyncedAt) {}
