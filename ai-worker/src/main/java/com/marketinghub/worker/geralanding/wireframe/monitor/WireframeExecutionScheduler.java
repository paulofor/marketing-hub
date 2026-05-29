package com.marketinghub.worker.geralanding.wireframe.monitor;


import com.marketinghub.worker.geralanding.wireframe.request.GeraLandingWireframeOpenAiExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda o processamento de jobs pendentes da etapa wireframe. */
@Component
public class WireframeExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(WireframeExecutionScheduler.class);

    private final GeraLandingWireframeOpenAiExecutionService executionService;
    private final WireframePendingJobsService pendingJobsService;
    private final int pendingLimit;

    public WireframeExecutionScheduler(GeraLandingWireframeOpenAiExecutionService executionService,
                                       WireframePendingJobsService pendingJobsService,
                                       @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.executionService = executionService;
        this.pendingJobsService = pendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa o ciclo da etapa wireframe consultando apenas o endpoint específico da etapa. */
    @Scheduled(cron = "0 */30 * * * *")
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            executionService.processExecutions(pendingJobsService.listPendingWireframeJobs(pendingLimit));
        } catch (Exception ex) {
            log.error("WireframeExecutionScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("WireframeExecutionScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
