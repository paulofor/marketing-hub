package com.marketinghub.worker.creativereview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: acionar periodicamente a etapa de melhoria de anúncios. */
@Component
public class CreativeImprovementScheduler {
    private static final Logger log = LoggerFactory.getLogger(CreativeImprovementScheduler.class);
    private final CreativeImprovementService service;
    private final int limit;

    /** Inicializa o agendamento com lote pequeno para limitar custo concorrente. */
    public CreativeImprovementScheduler(CreativeImprovementService service,
                                        @Value("${creative-improvement.worker.pending-limit:3}") int limit) {
        this.service = service;
        this.limit = Math.max(1, limit);
    }

    /** Executa a fila sem decidir aprovação ou publicação fora do backend. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        CreativeImprovementService.Summary summary = service.processPending(limit);
        log.info("CreativeImprovementScheduler concluído. total={} success={} failed={}",
                summary.total(), summary.success(), summary.failed());
    }
}
