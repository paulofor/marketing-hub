package com.marketinghub.facebookadsworker.facebookcampaign.publication;

import com.marketinghub.facebookadsworker.pipeline.StageContext;
import com.marketinghub.facebookadsworker.pipeline.StageProcessor;
import com.marketinghub.facebookadsworker.pipeline.StageResult;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete pipeline stage that processes one Facebook campaign publication request.
 */
public class CampaignPublicationProcessor implements StageProcessor<CampaignPublicationInput, CampaignPublicationOutput> {
    private final CampaignPublicationHandler handler;

    /**
     * Creates the publication processor with the handler that owns the Meta Ads side effects.
     */
    public CampaignPublicationProcessor(CampaignPublicationHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Processes one campaign publication stage and records lightweight execution metrics.
     */
    @Override
    public StageResult<CampaignPublicationOutput> process(StageContext<CampaignPublicationInput> context) {
        CampaignPublicationInput input = Objects.requireNonNull(context.input(), "input");
        handler.publish(input.experiment(), input.configuration());
        long experimentId = input.experiment() != null ? input.experiment().id() : -1L;
        return new StageResult<>(
            new CampaignPublicationOutput(experimentId, true),
            Map.of("experimentId", experimentId, "stageName", context.stageName())
        );
    }
}
