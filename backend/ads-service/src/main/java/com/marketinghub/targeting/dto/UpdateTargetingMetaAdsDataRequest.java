package com.marketinghub.targeting.dto;

public record UpdateTargetingMetaAdsDataRequest(
        String metaId,
        String metaKey,
        Long metaAudienceSizeLowerBound,
        Long metaAudienceSizeUpperBound
) {}
