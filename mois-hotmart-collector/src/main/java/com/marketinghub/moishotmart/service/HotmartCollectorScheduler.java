package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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

    @Scheduled(cron = "${collector.scheduler.cron:0 0 * * * *}")
    public void collectHourly() {
        if (!enabled) {
            log.info("Hotmart scheduler desabilitado por configuração.");
            return;
        }
        HotmartCollectionResponse response = collectorService.collect(new HotmartCollectionRequest(source, maxProducts));
        log.info("Hotmart scheduler executado status={} produtos={} mensagem={}",
                response.status(), response.products().size(), response.message());
    }
}
