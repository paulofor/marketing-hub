package com.marketinghub.facebookads.dto;

import com.marketinghub.facebookads.FacebookCampaignStopReason;

import java.time.Instant;

public record FacebookCampaignStopRequestDto(
        String id,
        String externalId,
        String adAccountId,
        Long experimentId,
        FacebookCampaignStopReason stopReason,
        Instant stopRequestedAt,
        String stopLastError
) {
}
