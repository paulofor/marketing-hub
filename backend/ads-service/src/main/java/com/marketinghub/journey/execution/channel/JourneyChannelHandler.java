package com.marketinghub.journey.execution.channel;

import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;

import java.util.Map;

/**
 * Contract for external channel adapters (Meta Ads, SendGrid, WhatsApp...).
 */
public interface JourneyChannelHandler {
    JourneyStimulusType supportedType();

    ChannelDispatchResult dispatch(JourneyAssignment assignment, JourneyStep step, Map<String, Object> context);
}
