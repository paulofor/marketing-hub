package com.marketinghub.openai;

import com.marketinghub.openai.service.OpenAiModelPricingSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: disparar a sincronização diária dos preços financeiros dos modelos OpenAI. */
@Component
public class OpenAiModelPricingScheduler {
    private static final Logger log = LoggerFactory.getLogger(OpenAiModelPricingScheduler.class);

    private final OpenAiModelPricingSyncService syncService;

    /** Inicializa o agendador com o serviço transacional de sincronização de preços. */
    public OpenAiModelPricingScheduler(OpenAiModelPricingSyncService syncService) {
        this.syncService = syncService;
    }

    /** Executa diariamente às 04:00 no fuso de São Paulo para manter os preços dos modelos atualizados. */
    @Scheduled(cron = "0 0 4 * * *", zone = "America/Sao_Paulo")
    public void syncDailyPricing() {
        try {
            int updated = syncService.syncOfficialPricing();
            log.info("Rotina diária de preços OpenAI concluída; operation=openai-pricing-daily-sync modelsUpdated={}", updated);
        } catch (RuntimeException ex) {
            StackTraceElement line = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : null;
            log.error(
                    "Falha na rotina diária de preços OpenAI; operation=openai-pricing-daily-sync line={} errorClass={} errorMessage={}",
                    line,
                    ex.getClass().getName(),
                    ex.getMessage(),
                    ex);
        }
    }
}
