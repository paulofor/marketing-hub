package com.marketinghub.facebookadsworker.pipeline;

import java.util.Map;
import java.util.Objects;

/**
 * Generic executor that invokes one concrete pipeline stage without knowing its implementation details.
 */
public class PipelineWorker<I, O> {
    private final StageProcessor<I, O> processor;

    /**
     * Creates a worker for the supplied stage processor.
     */
    public PipelineWorker(StageProcessor<I, O> processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    /**
     * Executes the configured stage with input, execution identity and optional configuration.
     */
    public StageResult<O> process(String stageName, String executionId, I input, Map<String, Object> config) {
        StageContext<I> context = new StageContext<>(stageName, executionId, input, config);
        return processor.process(context);
    }
}
