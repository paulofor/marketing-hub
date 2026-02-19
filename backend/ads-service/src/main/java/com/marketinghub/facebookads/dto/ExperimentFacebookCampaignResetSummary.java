package com.marketinghub.facebookads.dto;

/** Summary describing how many unpublished artifacts will be removed/reset. */
public record ExperimentFacebookCampaignResetSummary(
        long campaigns,
        long adSets,
        long ads,
        long creatives
) {
    public static ExperimentFacebookCampaignResetSummary empty() {
        return new ExperimentFacebookCampaignResetSummary(0, 0, 0, 0);
    }
}
