package com.marketinghub.worker.openai.core.port;

import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;

public interface OpenAiClientPort {

    OpenAiDispatch dispatch(OpenAiRequest request);

    OpenAiResult<String> awaitResult(OpenAiDispatch dispatch);
}
