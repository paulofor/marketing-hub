package com.marketinghub.moisclickbank.service;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsável por disparar os ciclos agendados do coletor Clickbank quando o agendamento estiver habilitado. */
@Component
public class ClickbankCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClickbankCollectorScheduler.class);

    private final ClickbankCollectorService collectorService;
    private final boolean enabled;
    private final String source;
    private final int maxProducts;

    /** Inicializa o agendador com serviço, chave de ativação, fonte e limite configurados. */
    public ClickbankCollectorScheduler(
            ClickbankCollectorService collectorService,
            @Value("${collector.scheduler.enabled:false}") boolean enabled,
            @Value("${collector.scheduler.source:clickbank-market}") String source,
            @Value("${collector.scheduler.max-products:25}") int maxProducts
    ) {
        this.collectorService = collectorService;
        this.enabled = enabled;
        this.source = source;
        this.maxProducts = maxProducts;
    }

    /** Executa o ciclo Clickbank correspondente ao horário atual quando o scheduler está ativo. */
    @Scheduled(cron = "${collector.scheduler.cron:0 0 * * * *}")
    public void collectHourly() {
        log.info("Iniciando execução agendada do Clickbank Collector source={} maxProducts={}", source, maxProducts);
        if (!enabled) {
            log.info("Clickbank scheduler desabilitado por configuração.");
            return;
        }
        int currentHour = LocalDateTime.now().getHour();
        int cycleSlot = currentHour % 3;
        ClickbankCollectionRequest request = new ClickbankCollectionRequest(source, maxProducts);
        ClickbankCollectionResponse response;
        String cycleName;
        if (cycleSlot == 0) {
            cycleName = "CICLO_1_TOP_OFFERS";
            response = collectorService.collectFirstCycle(request);
        } else if (cycleSlot == 1) {
            cycleName = "CICLO_2_VENDAS_PAGE";
            response = collectorService.collectSecondCycleFromBackend(request);
        } else {
            cycleName = "CICLO_3_GRAPHQL";
            response = collectorService.collectThirdCycleGraphql(request);
        }
        log.info("Clickbank scheduler executado hora={} ciclo={} status={} produtos={} mensagem={}",
                currentHour,
                cycleName,
                response.status(),
                response.products().size(),
                response.message());
    }
}
