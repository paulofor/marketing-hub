package com.marketinghub.facebookadsworker.metaaudience;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agendador responsável por acionar a criação de audiências de email na Meta. */
@Component
public class MetaAudienceSyncScheduler {
    private final MetaAudienceSyncService service;

    /** Cria o agendador com o serviço de sincronização de audiências. */
    public MetaAudienceSyncScheduler(MetaAudienceSyncService service) {
        this.service = service;
    }

    /** Executa periodicamente a fila interna de audiências prontas do backend. */
    @Scheduled(fixedDelayString = "${facebook.meta-audience.scheduler.delay:300000}")
    public void process() {
        service.processPendingAudiences();
    }
}
