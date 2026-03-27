package com.marketinghub.worker.hypothesisframework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HypothesisFrameworkGenerationScheduler {
    private static final Logger log = LoggerFactory.getLogger(HypothesisFrameworkGenerationScheduler.class);

    private final HypothesisFrameworkGenerationWorkerService service;

    public HypothesisFrameworkGenerationScheduler(HypothesisFrameworkGenerationWorkerService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        log.info("HypothesisFrameworkGenerationScheduler started");
        try {
            service.processPending();
        } finally {
            log.info("HypothesisFrameworkGenerationScheduler finished");
        }
    }
}
