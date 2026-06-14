package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Responsável por agendar e disparar os ciclos automáticos de coleta Hotmart.
 */
@Component
public class HotmartCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(HotmartCollectorScheduler.class);

    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

    private final HotmartCollectorService collectorService;
    private final boolean enabled;
    private final String source;
    private final int maxProducts;

    /**
     * Inicializa o agendador com o serviço de coleta e parâmetros operacionais configurados.
     */
    public HotmartCollectorScheduler(
            HotmartCollectorService collectorService,
            @Value("${collector.scheduler.enabled:true}") boolean enabled,
            @Value("${collector.scheduler.source:hotmart-market}") String source,
            @Value("${collector.scheduler.max-products:400}") int maxProducts
    ) {
        this.collectorService = collectorService;
        this.enabled = enabled;
        this.source = source;
        this.maxProducts = maxProducts;
    }

    /**
     * Executa o ciclo 1 de listagem de produtos uma única vez às 22:00 em 14 de junho de 2026, no horário de São Paulo.
     */
    @Scheduled(cron = "0 0 22 14 6 *", zone = "America/Sao_Paulo")
    public void collectFirstCycleAtTwentyTwoHundredOnJuneFourteenth2026() {
        if (!enabled) {
            log.info("Hotmart scheduler desabilitado por configuração.");
            return;
        }
        int currentYear = ZonedDateTime.now(SAO_PAULO_ZONE).getYear();
        if (currentYear != 2026) {
            log.info(
                    "Hotmart scheduler ciclo 1 ignorado porque o agendamento solicitado é exclusivo de 2026. anoAtual={}",
                    currentYear);
            return;
        }
        HotmartCollectionRequest request = new HotmartCollectionRequest(source, maxProducts);
        HotmartCollectionResponse response = collectorService.collectFirstCycle(request);
        log.info("Hotmart scheduler executado hora=22:00 dia=14/06 ano=2026 "
                        + "timezone=America/Sao_Paulo ciclo={} status={} produtos={} mensagem={}",
                "CICLO_1_LISTAGEM",
                response.status(),
                response.products().size(),
                response.message());
    }

    /**
     * Executa o ciclo 2 de enriquecimento de detalhes uma única vez às 12:20 em 13 de junho de 2026, no horário de São Paulo.
     */
    @Scheduled(cron = "0 20 12 13 6 *", zone = "America/Sao_Paulo")
    public void collectSecondCycleAtTwelveTwentyOnJuneThirteenth2026() {
        if (!enabled) {
            log.info("Hotmart scheduler desabilitado por configuração.");
            return;
        }
        int currentYear = ZonedDateTime.now(SAO_PAULO_ZONE).getYear();
        if (currentYear != 2026) {
            log.info(
                    "Hotmart scheduler ciclo 2 ignorado porque o agendamento solicitado é exclusivo de 2026. anoAtual={}",
                    currentYear);
            return;
        }
        HotmartCollectionRequest request = new HotmartCollectionRequest(source, maxProducts);
        HotmartCollectionResponse response = collectorService.collectSecondCycleFromBackend(request);
        log.info("Hotmart scheduler executado hora=12:20 dia=13/06 ano=2026 "
                        + "timezone=America/Sao_Paulo ciclo={} status={} produtos={} mensagem={}",
                "CICLO_2_DETALHES",
                response.status(),
                response.products().size(),
                response.message());
    }
}
