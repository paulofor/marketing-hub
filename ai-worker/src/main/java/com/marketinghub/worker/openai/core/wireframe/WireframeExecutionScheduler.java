package com.marketinghub.worker.openai.core.wireframe;

import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa wireframe no core OpenAI. */
public class WireframeExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(WireframeExecutionScheduler.class);

    private final StageWorker<WireframeInput, WireframeOutput> worker;
    private final WireframeWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de wireframe. */
    public WireframeExecutionScheduler(
            StageWorker<WireframeInput, WireframeOutput> worker,
            WireframeWorkerProperties properties
    ) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada cinco minutos o ciclo de processamento dos jobs pendentes de wireframe. */
    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());

        log.info(
                "Wireframe worker cycle finished. total={}, succeeded={}, failed={}",
                summary.total(),
                summary.succeeded(),
                summary.failed()
        );
    }
}
