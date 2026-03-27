package com.marketinghub.worker.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a varredura periódica das solicitações de aprendizado.
 */
@Component
public class ExperimentLearningScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExperimentLearningScheduler.class);
    private final ExperimentLearningJobService jobService;

    public ExperimentLearningScheduler(ExperimentLearningJobService jobService) {
        this.jobService = jobService;
    }

    @Scheduled(fixedDelayString = "${experiment.learning.fixed-delay:60000}")
    public void run() {
        log.debug("Iniciando varredura de solicitações de aprendizado");
        jobService.processPendingRequests();
    }
}
