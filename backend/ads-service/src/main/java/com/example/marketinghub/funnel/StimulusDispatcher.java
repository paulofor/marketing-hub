package com.example.marketinghub.funnel;

import com.example.marketinghub.model.Lead;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dispatches stimuli through external providers like Meta Graph, SendGrid or Twilio.
 */
@Component
@Slf4j
public class StimulusDispatcher {
    public void dispatch(FunnelStep step, Lead lead) {
        // In a real implementation this would call external providers.
        log.info("Dispatching {} to lead {} with step {}", step.getStimulusType(), lead.getId(), step.getId());
    }
}
