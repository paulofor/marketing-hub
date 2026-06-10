package com.marketinghub.facebookadsworker.facebooktargeting.metaads;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador responsável por acionar periodicamente a resolução de sinais de targeting na Meta Ads.
 */
@Component
public class MetaAdsTargetingEnrichmentScheduler {
    private final MetaAdsTargetingEnrichmentService service;

    /**
     * Inicializa o agendador com o serviço de enriquecimento Meta Ads.
     */
    public MetaAdsTargetingEnrichmentScheduler(MetaAdsTargetingEnrichmentService service) {
        this.service = service;
    }

    /**
     * Executa uma rodada de processamento dos elementos pendentes.
     */
    @Scheduled(fixedDelayString = "${facebook.targeting.metaads.scheduler.delay:300000}")
    public void process() {
        service.processPendingElements();
    }
}
