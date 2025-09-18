package com.marketinghub.journey.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically pulls journey assignments and triggers dispatch.
 */
@Component
@Slf4j
public class JourneyExecutionScheduler {
    private final JourneyExecutionService executionService;

    public JourneyExecutionScheduler(JourneyExecutionService executionService) {
        this.executionService = executionService;
    }

    @Scheduled(fixedDelayString = "#{@journeyExecutionProperties.pollInterval.toMillis()}")
    public void run() {
        try {
            executionService.processDueAssignments();
        } catch (Exception ex) {
            log.error("Unexpected error while processing journey assignments", ex);
        }
    }
}
