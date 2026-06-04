package com.marketinghub.nichocnae.routinesynthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara periodicamente a etapa seis para transformar sinais em cartão de rotina. */
@Component
public class RoutineSynthesizerScheduler {
    private static final Logger log = LoggerFactory.getLogger(RoutineSynthesizerScheduler.class);
    private final RoutineSynthesizerService service;

    /** Inicializa o scheduler com o serviço operacional da etapa seis. */
    public RoutineSynthesizerScheduler(RoutineSynthesizerService service) {
        this.service = service;
    }

    /** Executa a síntese de rotina em intervalo fixo para dar continuidade automática ao pipeline. */
    @Scheduled(cron = "0 */10 * * * *")
    public void runScheduled() {
        try {
            int processed = service.processPending("scheduler").size();
            if (processed > 0) {
                log.info("Etapa seis OPRM nichocnae executada pelo scheduler (processed={})", processed);
            }
        } catch (RuntimeException ex) {
            log.error("Erro no scheduler da etapa seis OPRM nichocnae", ex);
            throw ex;
        }
    }
}
