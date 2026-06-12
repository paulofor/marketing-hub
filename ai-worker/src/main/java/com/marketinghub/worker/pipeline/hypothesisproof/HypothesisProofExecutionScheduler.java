package com.marketinghub.worker.pipeline.hypothesisproof;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa Prova. */
public class HypothesisProofExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(HypothesisProofExecutionScheduler.class);
    private final PipelineWorker<HypothesisProofInput, HypothesisProofOutput> worker;
    private final HypothesisProofWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado da etapa Prova. */
    public HypothesisProofExecutionScheduler(
            PipelineWorker<HypothesisProofInput, HypothesisProofOutput> worker,
            HypothesisProofWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes da etapa Prova. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info(
                    "Hypothesis proof worker processed pending jobs. total={} success={} failure={}",
                    summary.total(),
                    summary.succeeded(),
                    summary.failed());
        }
    }
}
