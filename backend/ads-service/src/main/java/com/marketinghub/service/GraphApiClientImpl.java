package com.marketinghub.service;

import com.marketinghub.model.Lead;
import com.marketinghub.model.SequenceStep;
import com.marketinghub.model.SequenceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Trivial implementation that logs the welcome message.
 */
@Service
public class GraphApiClientImpl implements GraphApiClient {
    private static final Logger log = LoggerFactory.getLogger(GraphApiClientImpl.class);

    @Override
    @Async("taskExecutor")
    public void sendWelcomeAsync(Lead lead, SequenceTemplate template) {
        if (lead == null || template == null || template.getSteps() == null) {
            log.info("No lead or template provided");
            return;
        }
        for (SequenceStep step : template.getSteps()) {
            log.info("Sending step {} to lead {}: {}", step.getStepOrder(), lead.getId(), step.getContent());
            Integer delay = step.getDelaySeconds();
            if (delay != null && delay > 0) {
                try {
                    Thread.sleep(delay * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
