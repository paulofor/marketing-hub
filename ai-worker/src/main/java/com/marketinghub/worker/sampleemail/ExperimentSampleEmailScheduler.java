package com.marketinghub.worker.sampleemail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a execução periódica da geração de e-mails de amostra.
 */
@Component
public class ExperimentSampleEmailScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentSampleEmailScheduler.class);
    private final ExperimentSampleEmailService service;

    public ExperimentSampleEmailScheduler(ExperimentSampleEmailService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.info("ExperimentSampleEmailScheduler iniciado");
        try {
            service.generate();
        } catch (Exception ex) {
            log.error("ExperimentSampleEmailScheduler falhou", ex);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("ExperimentSampleEmailScheduler finalizado em {} ms", elapsed);
        }
    }
}
