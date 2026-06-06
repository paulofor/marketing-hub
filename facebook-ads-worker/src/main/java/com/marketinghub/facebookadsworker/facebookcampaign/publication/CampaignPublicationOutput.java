package com.marketinghub.facebookadsworker.facebookcampaign.publication;

/**
 * Output contract emitted after a Facebook campaign publication stage is processed.
 */
public record CampaignPublicationOutput(
    long experimentId,
    boolean processed
) {
}
