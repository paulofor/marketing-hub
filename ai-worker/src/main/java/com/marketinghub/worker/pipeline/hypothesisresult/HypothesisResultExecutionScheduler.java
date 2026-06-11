package com.marketinghub.worker.pipeline.hypothesisresult;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa Resultado. */
public class HypothesisResultExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(HypothesisResultExecutionScheduler.class);
    private final PipelineWorker<HypothesisResultInput, HypothesisResultOutput> worker;
    private final HypothesisResultWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado da etapa Resultado. */
    public HypothesisResultExecutionScheduler(
            PipelineWorker<HypothesisResultInput, HypothesisResultOutput> worker,
            HypothesisResultWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes da etapa Resultado. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info(
                    "Hypothesis result worker processed pending jobs. total={} success={} failure={}",
                    summary.total(),
                    summary.succeeded(),
                    summary.failed());
        }
    }
}
