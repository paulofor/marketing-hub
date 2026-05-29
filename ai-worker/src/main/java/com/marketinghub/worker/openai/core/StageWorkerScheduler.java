package com.marketinghub.worker.openai.core;

import com.marketinghub.worker.openai.core.model.ProcessingSummary;

import java.util.Objects;

public class StageWorkerScheduler {

    private final StageWorker<?, ?> worker;
    private final OpenAiWorkerProperties properties;

    public StageWorkerScheduler(StageWorker<?, ?> worker, OpenAiWorkerProperties properties) {
        this.worker = Objects.requireNonNull(worker, "worker must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public ProcessingSummary runOnce() {
        if (!properties.enabled()) {
            return ProcessingSummary.disabled();
        }
        return worker.processPending(properties.pendingLimit());
    }
}
