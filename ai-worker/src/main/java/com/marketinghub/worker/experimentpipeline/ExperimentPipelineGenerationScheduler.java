package com.marketinghub.worker.experimentpipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExperimentPipelineGenerationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineGenerationScheduler.class);

    private final ExperimentPipelineGenerationWorkerService service;

    public ExperimentPipelineGenerationScheduler(ExperimentPipelineGenerationWorkerService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            service.processPending();
        } catch (Exception ex) {
            log.error("ExperimentPipelineGenerationScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("ExperimentPipelineGenerationScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
