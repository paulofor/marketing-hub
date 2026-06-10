package com.marketinghub.worker.pipeline.hypothesispain;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa Dor. */
public class HypothesisPainExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(HypothesisPainExecutionScheduler.class);
    private final PipelineWorker<HypothesisPainInput, HypothesisPainOutput> worker;
    private final HypothesisPainWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado da etapa Dor. */
    public HypothesisPainExecutionScheduler(
            PipelineWorker<HypothesisPainInput, HypothesisPainOutput> worker,
            HypothesisPainWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes da etapa Dor. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info(
                    "Hypothesis pain worker processed pending jobs. total={} success={} failure={}",
                    summary.total(),
                    summary.succeeded(),
                    summary.failed());
        }
    }
}
