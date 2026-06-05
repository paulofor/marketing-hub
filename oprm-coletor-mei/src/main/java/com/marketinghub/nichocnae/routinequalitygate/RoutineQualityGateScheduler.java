package com.marketinghub.nichocnae.routinequalitygate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara periodicamente a etapa sete para decidir se cartões de rotina seguem para hipótese. */
@Component
public class RoutineQualityGateScheduler {
    private static final Logger log = LoggerFactory.getLogger(RoutineQualityGateScheduler.class);
    private final RoutineQualityGateService service;

    /** Inicializa o scheduler com o serviço operacional da etapa sete. */
    public RoutineQualityGateScheduler(RoutineQualityGateService service) {
        this.service = service;
    }

    /** Executa o gate de qualidade em intervalo fixo para dar continuidade automática ao pipeline. */
    @Scheduled(cron = "0 */10 * * * *")
    public void runScheduled() {
        try {
            int processed = service.processPending("scheduler").size();
            if (processed > 0) {
                log.info("Etapa sete OPRM nichocnae executada pelo scheduler (processed={})", processed);
            }
        } catch (RuntimeException ex) {
            log.error("Erro no scheduler da etapa sete OPRM nichocnae", ex);
            throw ex;
        }
    }
}
