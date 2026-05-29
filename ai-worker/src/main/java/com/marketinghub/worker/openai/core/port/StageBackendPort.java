package com.marketinghub.worker.openai.core.port;

import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;

import java.util.List;

public interface StageBackendPort<I, O> {

    List<StageExecution<I>> listPending(int limit);

    void markDispatched(StageExecution<I> execution, OpenAiDispatch dispatch);

    void markCompleted(StageExecution<I> execution, OpenAiResult<O> result);

    void markFailed(StageExecution<I> execution, Throwable error);
}
