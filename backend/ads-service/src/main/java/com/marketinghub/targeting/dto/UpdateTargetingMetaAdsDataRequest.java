package com.marketinghub.targeting.dto;

public record UpdateTargetingMetaAdsDataRequest(
        String metaId,
        Long metaAudienceSizeLowerBound,
        Long metaAudienceSizeUpperBound
) {}
