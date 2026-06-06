package com.marketinghub.facebookadsworker.facebookcampaign.publication;

import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.facebookcampaign.FacebookCampaignService.Experiment;

/**
 * Port used by the publication stage to delegate the concrete Meta Ads publication action.
 */
@FunctionalInterface
public interface CampaignPublicationHandler {

    /**
     * Publishes or skips one experiment according to the campaign publication rules.
     */
    void publish(Experiment experiment, FacebookWorkerConfiguration configuration);
}
