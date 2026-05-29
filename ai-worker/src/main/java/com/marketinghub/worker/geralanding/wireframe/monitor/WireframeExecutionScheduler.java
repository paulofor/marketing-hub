package com.marketinghub.worker.geralanding.wireframe.monitor;

import com.marketinghub.worker.geralanding.wireframe.request.GeraLandingWireframeOpenAiExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mantém o ciclo manual de processamento de jobs pendentes da etapa wireframe, sem agendamento automático. */
public class WireframeExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(WireframeExecutionScheduler.class);

    private final GeraLandingWireframeOpenAiExecutionService executionService;
    private final WireframePendingJobsService pendingJobsService;
    private final int pendingLimit;

    /** Recebe os serviços necessários para executar manualmente o ciclo da etapa wireframe. */
    public WireframeExecutionScheduler(GeraLandingWireframeOpenAiExecutionService executionService,
                                       WireframePendingJobsService pendingJobsService,
                                       int pendingLimit) {
        this.executionService = executionService;
        this.pendingJobsService = pendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa manualmente o ciclo da etapa wireframe consultando apenas o endpoint específico da etapa. */
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
