package com.marketinghub.worker.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a geração de e-mails da jornada.
 */
@Component
public class ExperimentEmailScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentEmailScheduler.class);
    private final ExperimentEmailService service;

    public ExperimentEmailScheduler(ExperimentEmailService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.info("ExperimentEmailScheduler iniciado");
        try {
            service.generate();
        } catch (Exception ex) {
            log.error("ExperimentEmailScheduler falhou", ex);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("ExperimentEmailScheduler finalizado em {} ms", elapsed);
        }
    }
}
