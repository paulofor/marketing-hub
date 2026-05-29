package com.marketinghub.worker.openai.core.model;

import java.math.BigDecimal;

public record OpenAiResult<O>(
        String openAiJobId,
        String rawResponse,
        String modelResponse,
        O parsedResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd
) {
    public <T> OpenAiResult<T> withParsedResponse(T parsed) {
        return new OpenAiResult<>(
                openAiJobId,
                rawResponse,
                modelResponse,
                parsed,
                inputTokens,
                outputTokens,
                costUsd
        );
    }
}
