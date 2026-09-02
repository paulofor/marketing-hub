package com.marketinghub.worker.experimentpipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: iniciar periodicamente o consumo da fila de conteúdo comercial dos experimentos. */
@Component
public class ExperimentPipelineGenerationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineGenerationScheduler.class);

    private final ExperimentPipelineGenerationWorkerService service;

    /** Configura o serviço que processa os jobs pendentes da fila. */
    public ExperimentPipelineGenerationScheduler(ExperimentPipelineGenerationWorkerService service) {
        this.service = service;
    }

    /** Executa um ciclo da fila em agendador exclusivo para evitar starvation por outras integrações. */
    @Scheduled(cron = "0 */1 * * * *", scheduler = "experimentPipelineTaskScheduler")
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
