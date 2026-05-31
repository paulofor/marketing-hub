package com.marketinghub.worker.openai.core.presetdesign;

import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o processamento periódico dos jobs pendentes da etapa presetdesign no core OpenAI. */
public class PresetDesignExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PresetDesignExecutionScheduler.class);

    private final StageWorker<PresetDesignInput, PresetDesignOutput> worker;
    private final PresetDesignWorkerProperties properties;

    /** Recebe o worker e as propriedades usadas pelo ciclo agendado de presetdesign. */
    public PresetDesignExecutionScheduler(
            StageWorker<PresetDesignInput, PresetDesignOutput> worker,
            PresetDesignWorkerProperties properties
    ) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada um minuto o ciclo de processamento dos jobs pendentes de presetdesign. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());

        log.info(
                "PresetDesign worker cycle finished. total={}, succeeded={}, failed={}",
                summary.total(),
                summary.succeeded(),
                summary.failed()
        );
    }
}
