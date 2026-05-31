package com.marketinghub.worker.openai.core.imagegeneration;

import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa imagegeneration no core OpenAI. */
public class ImageGenerationExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationExecutionScheduler.class);

    private final StageWorker<ImageGenerationInput, ImageGenerationOutput> worker;
    private final ImageGenerationWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de imagegeneration. */
    public ImageGenerationExecutionScheduler(
            StageWorker<ImageGenerationInput, ImageGenerationOutput> worker,
            ImageGenerationWorkerProperties properties
    ) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes de imagegeneration. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        log.info(
                "ImageGeneration worker cycle finished. total={}, succeeded={}, failed={}",
                summary.total(),
                summary.succeeded(),
                summary.failed()
        );
    }
}
