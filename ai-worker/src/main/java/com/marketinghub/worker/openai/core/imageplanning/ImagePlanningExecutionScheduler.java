package com.marketinghub.worker.openai.core.imageplanning;

import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa imageplanning no core OpenAI. */
public class ImagePlanningExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImagePlanningExecutionScheduler.class);

    private final StageWorker<ImagePlanningInput, ImagePlanningOutput> worker;
    private final ImagePlanningWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de imageplanning. */
    public ImagePlanningExecutionScheduler(
            StageWorker<ImagePlanningInput, ImagePlanningOutput> worker,
            ImagePlanningWorkerProperties properties
    ) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes de imageplanning. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());

        log.info(
                "ImagePlanning worker cycle finished. total={}, succeeded={}, failed={}",
                summary.total(),
                summary.succeeded(),
                summary.failed()
        );
    }
}
