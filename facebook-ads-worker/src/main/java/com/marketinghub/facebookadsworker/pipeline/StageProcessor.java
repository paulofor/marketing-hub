package com.marketinghub.facebookadsworker.pipeline;

/**
 * Contract implemented by each concrete and replaceable pipeline stage.
 */
public interface StageProcessor<I, O> {

    /**
     * Executes a pipeline stage using the provided context and returns its structured result.
     */
    StageResult<O> process(StageContext<I> context);
}
