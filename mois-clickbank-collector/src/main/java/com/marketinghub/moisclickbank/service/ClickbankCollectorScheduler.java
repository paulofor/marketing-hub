package com.marketinghub.moisclickbank.service;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClickbankCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClickbankCollectorScheduler.class);

    private final ClickbankCollectorService collectorService;
    private final boolean enabled;
    private final String source;
    private final int maxProducts;

    public ClickbankCollectorScheduler(
            ClickbankCollectorService collectorService,
            @Value("${collector.scheduler.enabled:true}") boolean enabled,
            @Value("${collector.scheduler.source:clickbank-market}") String source,
            @Value("${collector.scheduler.max-products:25}") int maxProducts
    ) {
        this.collectorService = collectorService;
        this.enabled = enabled;
        this.source = source;
        this.maxProducts = maxProducts;
    }

    @Scheduled(cron = "${collector.scheduler.cron:0 0 * * * *}")
    public void collectHourly() {
        log.info("Iniciando execução agendada do Clickbank Collector source={} maxProducts={}", source, maxProducts);
        if (!enabled) {
            log.info("Clickbank scheduler desabilitado por configuração.");
            return;
        }
        int currentHour = LocalDateTime.now().getHour();
        boolean isOddHour = currentHour % 2 != 0;
        ClickbankCollectionRequest request = new ClickbankCollectionRequest(source, maxProducts);
        ClickbankCollectionResponse response = isOddHour
                ? collectorService.collectFirstCycle(request)
                : collectorService.collectSecondCycleFromBackend(request);
        log.info("Clickbank scheduler executado hora={} ciclo={} status={} produtos={} mensagem={}",
                currentHour,
                isOddHour ? "CICLO_1_TOP_OFFERS" : "CICLO_2_VENDAS_PAGE",
                response.status(),
                response.products().size(),
                response.message());
    }
}
