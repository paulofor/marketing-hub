package com.marketinghub.worker.geralanding.imageplanning;

import com.marketinghub.worker.geralanding.GeraLandingExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda o processamento de jobs pendentes da etapa image-planning. */
@Component
public class ImagePlanningExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(ImagePlanningExecutionScheduler.class);

    private final GeraLandingExecutionService executionService;
    private final ImagePlanningPendingJobsService pendingJobsService;
    private final int pendingLimit;

    public ImagePlanningExecutionScheduler(GeraLandingExecutionService executionService,
                                           ImagePlanningPendingJobsService pendingJobsService,
                                           @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.executionService = executionService;
        this.pendingJobsService = pendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa o ciclo da etapa image-planning consultando apenas o endpoint específico da etapa. */
    @Scheduled(cron = "20 */1 * * * *")
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            executionService.processExecutions(pendingJobsService.listPendingImagePlanningJobs(pendingLimit));
        } catch (Exception ex) {
            log.error("ImagePlanningExecutionScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("ImagePlanningExecutionScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
