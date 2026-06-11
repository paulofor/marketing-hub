package com.marketinghub.worker.pipeline.hypothesisoffer;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa Oferta. */
public class HypothesisOfferExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(HypothesisOfferExecutionScheduler.class);
    private final PipelineWorker<HypothesisOfferInput, HypothesisOfferOutput> worker;
    private final HypothesisOfferWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado da etapa Oferta. */
    public HypothesisOfferExecutionScheduler(
            PipelineWorker<HypothesisOfferInput, HypothesisOfferOutput> worker,
            HypothesisOfferWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes da etapa Oferta. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info(
                    "Hypothesis offer worker processed pending jobs. total={} success={} failure={}",
                    summary.total(),
                    summary.succeeded(),
                    summary.failed());
        }
    }
}
