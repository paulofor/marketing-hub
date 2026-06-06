package com.marketinghub.facebookadsworker.facebookcampaign.publication;

import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.facebookcampaign.FacebookCampaignService.Experiment;

/**
 * Input contract for the Facebook campaign publication stage.
 */
public record CampaignPublicationInput(
    Experiment experiment,
    FacebookWorkerConfiguration configuration
) {
}
