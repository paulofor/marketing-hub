package com.marketinghub.worker.openai.core.port;

public interface StageResponseValidator<O> {

    O validateAndParse(String modelResponse);
}
