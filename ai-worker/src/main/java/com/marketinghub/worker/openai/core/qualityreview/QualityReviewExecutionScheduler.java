package com.marketinghub.worker.openai.core.qualityreview;

import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da revisão visual. */
public class QualityReviewExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(QualityReviewExecutionScheduler.class);

    private final StageWorker<QualityReviewInput, QualityReviewOutput> worker;
    private final QualityReviewWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de revisão visual. */
    public QualityReviewExecutionScheduler(StageWorker<QualityReviewInput, QualityReviewOutput> worker, QualityReviewWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes de revisão visual. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        log.info("Quality review worker cycle finished. total={}, succeeded={}, failed={}", summary.total(), summary.succeeded(), summary.failed());
    }
}
