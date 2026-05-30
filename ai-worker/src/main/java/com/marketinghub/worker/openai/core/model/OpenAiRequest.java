package com.marketinghub.worker.openai.core.model;

import java.util.Map;
import java.util.Objects;

/** Responsabilidade: transportar o request OpenAI montado com dados de auditoria da etapa. */
public record OpenAiRequest(
        String model,
        String prompt,
        String requestBodyJson,
        String schemaName,
        String schemaJson,
        String promptMarkdownContent,
        Map<String, Object> metadata
) {
    /** Valida os campos obrigatórios e normaliza metadados opcionais. */
    public OpenAiRequest {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(requestBodyJson, "requestBodyJson must not be null");
        Objects.requireNonNull(promptMarkdownContent, "promptMarkdownContent must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
