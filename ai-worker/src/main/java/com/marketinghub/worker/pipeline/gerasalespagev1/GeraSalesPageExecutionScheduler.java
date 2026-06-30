package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico do pipeline GeraSalesPage v1 no AI Worker. */
public class GeraSalesPageExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(GeraSalesPageExecutionScheduler.class);
    private final PipelineWorker<GeraSalesPageInput, GeraSalesPageOutput> worker;
    private final GeraSalesPageWorkerProperties properties;

    /** Recebe o worker genérico e propriedades do ciclo operacional. */
    public GeraSalesPageExecutionScheduler(
            PipelineWorker<GeraSalesPageInput, GeraSalesPageOutput> worker,
            GeraSalesPageWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de consumo de pendências do GeraSalesPage v1. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info(
                    "GeraSalesPage v1 worker processed pending jobs. total={} success={} failure={}",
                    summary.total(),
                    summary.succeeded(),
                    summary.failed());
        }
    }
}
