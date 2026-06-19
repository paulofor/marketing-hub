package com.marketinghub.nichocnae.evidencelevelgate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda no executor externo o processamento periódico da etapa onze E0-E5. */
@Component
public class EvidenceLevelGateScheduler {
    private static final Logger log = LoggerFactory.getLogger(EvidenceLevelGateScheduler.class);
    private final EvidenceLevelGateService service;

    /** Inicializa o scheduler com o serviço da etapa onze. */
    public EvidenceLevelGateScheduler(EvidenceLevelGateService service) { this.service = service; }

    /** Executa a etapa onze em polling controlado pelo executor, nunca pelo backend. */
    @Scheduled(cron = "0 */2 * * * *")
    public void run() {
        int processed = service.processPending();
        if (processed > 0) log.info("Etapa onze E0-E5 OPRM nichocnae executada pelo scheduler (processed={})", processed);
    }
}
