package com.marketinghub.worker.experimentpromise;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a busca periódica de promessas pendentes no backend. */
@Component
public class ExperimentPromiseScheduler {
    private final ExperimentPromiseWorkerService service;

    /** Inicializa o agendador com o serviço de processamento de promessas. */
    public ExperimentPromiseScheduler(ExperimentPromiseWorkerService service) {
        this.service = service;
    }

    /** Executa o processamento periódico da fila de promessa única. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        service.processPending();
    }
}
