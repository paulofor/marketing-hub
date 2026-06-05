package com.marketinghub.nichocnae.enrichednichematerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara periodicamente a etapa final que materializa nicho e nicho enriquecido. */
@Component
public class EnrichedNicheMaterializerScheduler {
    private static final Logger log = LoggerFactory.getLogger(EnrichedNicheMaterializerScheduler.class);
    private final EnrichedNicheMaterializerService service;

    /** Inicializa o scheduler com o serviço operacional da etapa final. */
    public EnrichedNicheMaterializerScheduler(EnrichedNicheMaterializerService service) {
        this.service = service;
    }

    /** Executa a materialização final em intervalo fixo para fechar o pipeline NichoCNAE. */
    @Scheduled(cron = "0 */10 * * * *")
    public void runScheduled() {
        try {
            int processed = service.processPending("scheduler").size();
            if (processed > 0) {
                log.info("Etapa final OPRM nichocnae executada pelo scheduler (processed={})", processed);
            }
        } catch (RuntimeException ex) {
            log.error("Erro no scheduler da etapa final OPRM nichocnae", ex);
            throw ex;
        }
    }
}
