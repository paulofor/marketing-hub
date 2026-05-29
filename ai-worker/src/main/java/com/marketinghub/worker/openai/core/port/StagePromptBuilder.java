package com.marketinghub.worker.openai.core.port;

import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;

public interface StagePromptBuilder<I> {

    OpenAiRequest build(StageExecution<I> execution);
}
