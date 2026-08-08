package com.marketinghub.worker.creativereview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: acionar periodicamente a fila do agente especialista em anúncios. */
@Component
public class CreativeReviewScheduler {
    private static final Logger log = LoggerFactory.getLogger(CreativeReviewScheduler.class);
    private final CreativeReviewService service;
    private final int limit;

    /** Inicializa o agendamento com limite controlado por lote. */
    public CreativeReviewScheduler(CreativeReviewService service,
                                   @Value("${creative-review.worker.pending-limit:3}") int limit) {
        this.service = service;
        this.limit = Math.max(1, limit);
    }

    /** Executa a revisão a cada minuto sem decidir avanço de pipeline fora do backend. */
    @Scheduled(cron = "30 */1 * * * *")
    public void run() {
        CreativeReviewService.Summary summary = service.processPending(limit);
        log.info("CreativeReviewScheduler concluído. total={} success={} failed={}",
                summary.total(), summary.success(), summary.failed());
    }
}
