package com.marketinghub.worker.creative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: disparar periodicamente o processamento da fila de criativos de experimentos.
 */
@Component
public class CreativeGenerationScheduler {
    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationScheduler.class);

    private final CreativeGenerationService service;
    private final int pendingLimit;

    /** Inicializa o scheduler com o serviço de geração e o limite de itens por ciclo. */
    public CreativeGenerationScheduler(
            CreativeGenerationService service,
            @Value("${creative.generation.pending-limit:5}") int pendingLimit
    ) {
        this.service = service;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    /** Executa a cada minuto a fila de criativos pendentes. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        CreativeGenerationService.ProcessingSummary summary = service.processPending(pendingLimit);
        log.info("CreativeGenerationScheduler finished. total={} succeeded={} failed={}",
                summary.total(), summary.succeeded(), summary.failed());
    }
}
