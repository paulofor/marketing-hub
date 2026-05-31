package com.marketinghub.worker.openai.core.copy;

import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa copy no core OpenAI. */
public class CopyExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CopyExecutionScheduler.class);

    private final StageWorker<CopyInput, CopyOutput> worker;
    private final CopyWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de copy. */
    public CopyExecutionScheduler(
            StageWorker<CopyInput, CopyOutput> worker,
            CopyWorkerProperties properties
    ) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes de copy. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());

        log.info(
                "Copy worker cycle finished. total={}, succeeded={}, failed={}",
                summary.total(),
                summary.succeeded(),
                summary.failed()
        );
    }
}
