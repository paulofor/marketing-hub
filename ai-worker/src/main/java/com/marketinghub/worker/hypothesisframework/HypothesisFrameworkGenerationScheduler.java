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
        long startedAt = System.currentTimeMillis();
        log.info("HypothesisFrameworkGenerationScheduler started (thread={})", Thread.currentThread().getName());
        try {
            service.processPending();
            log.info("HypothesisFrameworkGenerationScheduler cycle completed successfully");
        } catch (Exception ex) {
            log.error("HypothesisFrameworkGenerationScheduler cycle failed", ex);
            throw ex;
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("HypothesisFrameworkGenerationScheduler finished (elapsedMs={})", elapsedMs);
        }
    }
}
