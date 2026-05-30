package com.marketinghub.worker.geralanding.deliverables.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mantém desligado o agendamento automático de jobs pendentes da etapa deliverables. */
@Component
public class DeliverablesExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(DeliverablesExecutionScheduler.class);

    private final DeliverablesExecutionProcessor processor;
    private final DeliverablesPendingJobsService pendingJobsService;
    private final int pendingLimit;

    /** Configura as dependências da etapa deliverables mantendo limite mínimo de um job. */
    public DeliverablesExecutionScheduler(DeliverablesExecutionProcessor processor,
                                          DeliverablesPendingJobsService pendingJobsService,
                                          @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.processor = processor;
        this.pendingJobsService = pendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa manualmente o ciclo da etapa deliverables consultando apenas o endpoint específico da etapa. */
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            processor.processExecutions(pendingJobsService.listPendingDeliverablesJobs(pendingLimit));
        } catch (Exception ex) {
            log.error("DeliverablesExecutionScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("DeliverablesExecutionScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
