package com.marketinghub.worker.openai.core.model;

import java.util.Map;
import java.util.Objects;

public record OpenAiRequest(
        String model,
        String prompt,
        String requestBodyJson,
        String schemaName,
        String schemaJson,
        Map<String, Object> metadata
) {
    public OpenAiRequest {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(requestBodyJson, "requestBodyJson must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
