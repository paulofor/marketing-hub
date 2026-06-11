package com.marketinghub.worker.pipeline.hypothesismechanism;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa Mecanismo. */
public class HypothesisMechanismExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(HypothesisMechanismExecutionScheduler.class);
    private final PipelineWorker<HypothesisMechanismInput, HypothesisMechanismOutput> worker;
    private final HypothesisMechanismWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado da etapa Mecanismo. */
    public HypothesisMechanismExecutionScheduler(
            PipelineWorker<HypothesisMechanismInput, HypothesisMechanismOutput> worker,
            HypothesisMechanismWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes da etapa Mecanismo. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info(
                    "Hypothesis mechanism worker processed pending jobs. total={} success={} failure={}",
                    summary.total(),
                    summary.succeeded(),
                    summary.failed());
        }
    }
}
