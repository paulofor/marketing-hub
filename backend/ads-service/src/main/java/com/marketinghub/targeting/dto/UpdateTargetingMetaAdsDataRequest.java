package com.marketinghub.targeting.dto;

/**
 * Contrato interno para atualizar o resultado de enriquecimento Meta Ads de um elemento de segmentação.
 */
public record UpdateTargetingMetaAdsDataRequest(
        String metaId,
        String metaKey,
        Long metaAudienceSizeLowerBound,
        Long metaAudienceSizeUpperBound,
        Boolean metaIdUnavailable,
        String metaIdUnavailableReason
) {}
