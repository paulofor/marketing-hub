package com.marketinghub.worker.openai.core.port;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;

public interface StageResponseHandler<I, O> {

    default void handleSuccess(StageExecution<I> execution, OpenAiResult<O> result) {
        // no-op
    }

    default void handleFailure(StageExecution<I> execution, Throwable error) {
        // no-op
    }

    static <I, O> StageResponseHandler<I, O> noOp() {
        return new StageResponseHandler<>() {};
    }
}
