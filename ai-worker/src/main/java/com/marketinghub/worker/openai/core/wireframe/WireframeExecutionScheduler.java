package com.marketinghub.worker.openai.core.wireframe;

import com.marketinghub.worker.openai.core.OpenAiWorkerProperties;
import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WireframeExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(WireframeExecutionScheduler.class);

    private final StageWorker<WireframeInput, WireframeOutput> worker;
    private final OpenAiWorkerProperties properties;

    public WireframeExecutionScheduler(
            StageWorker<WireframeInput, WireframeOutput> worker,
            OpenAiWorkerProperties properties
    ) {
        this.worker = worker;
        this.properties = properties;
    }

    @Scheduled(cron = "${wireframe.worker.cron:0 */30 * * * *}")
    public void run() {
        if (!properties.enabled()) {
            log.debug("Wireframe worker disabled.");
            return;
        }

        ProcessingSummary summary = worker.processPending(properties.pendingLimit());

        log.info(
                "Wireframe worker cycle finished. total={}, succeeded={}, failed={}",
                summary.total(),
                summary.succeeded(),
                summary.failed()
        );
    }
}
