package com.marketinghub.worker.geralanding.copy.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mantém desligado o agendamento automático de jobs pendentes da etapa copy. */
@Component
public class CopyExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CopyExecutionScheduler.class);

    private final CopyExecutionProcessor processor;
    private final CopyPendingJobsService pendingJobsService;
    private final int pendingLimit;

    /** Configura as dependências da etapa copy mantendo limite mínimo de um job. */
    public CopyExecutionScheduler(CopyExecutionProcessor processor,
                                  CopyPendingJobsService pendingJobsService,
                                  @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.processor = processor;
        this.pendingJobsService = pendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa manualmente o ciclo da etapa copy consultando apenas o endpoint específico da etapa. */
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            processor.processExecutions(pendingJobsService.listPendingCopyJobs(pendingLimit));
        } catch (Exception ex) {
            log.error("CopyExecutionScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("CopyExecutionScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
