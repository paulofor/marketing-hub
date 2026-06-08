package com.marketinghub.worker.pipeline.deliverables;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa deliverables no pipeline genérico. */
public class DeliverablesExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(DeliverablesExecutionScheduler.class);
    private final PipelineWorker<DeliverablesInput, DeliverablesOutput> worker;
    private final DeliverablesWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de deliverables. */
    public DeliverablesExecutionScheduler(PipelineWorker<DeliverablesInput, DeliverablesOutput> worker, DeliverablesWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes de deliverables. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        log.info(
                "Deliverables pipeline worker cycle finished. enabled={} total={} succeeded={} failed={} processedAt={}",
                summary.enabled(),
                summary.total(),
                summary.succeeded(),
                summary.failed(),
                summary.processedAt());
    }
}
