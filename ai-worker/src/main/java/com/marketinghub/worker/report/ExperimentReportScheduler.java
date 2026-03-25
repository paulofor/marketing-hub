package com.marketinghub.worker.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a geração dos relatórios de experimento.
 */
@Component
public class ExperimentReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExperimentReportScheduler.class);
    private final ExperimentReportService service;

    public ExperimentReportScheduler(ExperimentReportService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${experiment.report.fixed-delay:60000}")
    public void run() {
        log.debug("Iniciando varredura de solicitações de relatório");
        service.processPendingRequests();
    }
}
