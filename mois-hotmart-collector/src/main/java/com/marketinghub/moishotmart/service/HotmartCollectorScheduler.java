package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
/**
 * Responsável por agendar e disparar os ciclos automáticos de coleta Hotmart.
 */
public class HotmartCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(HotmartCollectorScheduler.class);

    private final HotmartCollectorService collectorService;
    private final boolean enabled;
    private final String source;
    private final int maxProducts;

    public HotmartCollectorScheduler(
            HotmartCollectorService collectorService,
            @Value("${collector.scheduler.enabled:true}") boolean enabled,
            @Value("${collector.scheduler.source:hotmart-market}") String source,
            @Value("${collector.scheduler.max-products:25}") int maxProducts
    ) {
        this.collectorService = collectorService;
        this.enabled = enabled;
        this.source = source;
        this.maxProducts = maxProducts;
    }

    /**
     * Executa o ciclo 1 de listagem de produtos diariamente às 23:20.
     */
    @Scheduled(cron = "0 20 23 * * *")
    public void collectFirstCycleAtTwentyThreeTwenty() {
        if (!enabled) {
            log.info("Hotmart scheduler desabilitado por configuração.");
            return;
        }
        HotmartCollectionRequest request = new HotmartCollectionRequest(source, maxProducts);
        HotmartCollectionResponse response = collectorService.collectFirstCycle(request);
        log.info("Hotmart scheduler executado hora=23:20 ciclo={} status={} produtos={} mensagem={}",
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
