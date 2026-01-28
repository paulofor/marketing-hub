package com.marketinghub.facebookads.dto;

/**
 * Minimal projection representing a campaign tied to a running experiment.
 */
public record RunningCampaignDto(
        String campaignId,
        String adAccountId,
        Long experimentId,
        String experimentName
) {}
