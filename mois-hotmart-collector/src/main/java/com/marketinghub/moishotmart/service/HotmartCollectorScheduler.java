package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
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
     * Executa o próximo ciclo 1 de listagem de produtos às 13:30 no dia 1 de junho, no horário de São Paulo.
     */
    @Scheduled(cron = "0 30 13 1 6 *", zone = "America/Sao_Paulo")
    public void collectFirstCycleAtThirteenThirtyOnJuneFirst() {
        if (!enabled) {
            log.info("Hotmart scheduler desabilitado por configuração.");
            return;
        }
        HotmartCollectionRequest request = new HotmartCollectionRequest(source, maxProducts);
        HotmartCollectionResponse response = collectorService.collectFirstCycle(request);
        log.info("Hotmart scheduler executado hora=13:30 dia=01/06 timezone=America/Sao_Paulo ciclo={} status={} produtos={} mensagem={}",
                "CICLO_1_LISTAGEM",
                response.status(),
                response.products().size(),
                response.message());
    }

    /**
     * Executa o ciclo 2 de enriquecimento de detalhes diariamente às 17:00.
     */
    @Scheduled(cron = "0 0 17 * * *")
    public void collectSecondCycleAtSeventeen() {
        if (!enabled) {
            log.info("Hotmart scheduler desabilitado por configuração.");
            return;
        }
        HotmartCollectionRequest request = new HotmartCollectionRequest(source, maxProducts);
        HotmartCollectionResponse response = collectorService.collectSecondCycleFromBackend(request);
        log.info("Hotmart scheduler executado hora=17 ciclo={} status={} produtos={} mensagem={}",
                "CICLO_2_DETALHES",
                response.status(),
                response.products().size(),
                response.message());
    }
}
