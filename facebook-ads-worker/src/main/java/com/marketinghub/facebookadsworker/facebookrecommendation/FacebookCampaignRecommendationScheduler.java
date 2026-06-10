package com.marketinghub.facebookadsworker.facebookrecommendation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a coleta periódica de sugestões oficiais da Meta para campanhas ativas.
 */
@Component
public class FacebookCampaignRecommendationScheduler {
    private final FacebookCampaignRecommendationService service;

    /**
     * Cria o agendador com o serviço de coleta de sugestões.
     */
    public FacebookCampaignRecommendationScheduler(FacebookCampaignRecommendationService service) {
        this.service = service;
    }

    /**
     * Executa a coleta a cada trinta minutos com cron explícito.
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void scheduleRecommendationSync() {
        service.syncActiveCampaignRecommendations();
    }
}
