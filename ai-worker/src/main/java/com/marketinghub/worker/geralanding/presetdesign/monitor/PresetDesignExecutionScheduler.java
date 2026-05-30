package com.marketinghub.worker.geralanding.presetdesign.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mantém desligado o agendamento automático de jobs pendentes da etapa design-preset. */
@Component
public class PresetDesignExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(PresetDesignExecutionScheduler.class);

    private final PresetDesignExecutionProcessor processor;
    private final PresetDesignPendingJobsService pendingJobsService;
    private final int pendingLimit;

    /** Configura as dependências da etapa design-preset mantendo limite mínimo de um job. */
    public PresetDesignExecutionScheduler(PresetDesignExecutionProcessor processor,
                                          PresetDesignPendingJobsService pendingJobsService,
                                          @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.processor = processor;
        this.pendingJobsService = pendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa manualmente o ciclo da etapa design-preset consultando apenas o endpoint específico da etapa. */
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            processor.processExecutions(pendingJobsService.listPendingPresetDesignJobs(pendingLimit));
        } catch (Exception ex) {
            log.error("PresetDesignExecutionScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("PresetDesignExecutionScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
