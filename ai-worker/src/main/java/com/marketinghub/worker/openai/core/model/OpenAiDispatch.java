package com.marketinghub.worker.openai.core.model;

import java.time.Instant;
import java.util.Objects;

/** Responsabilidade: transportar os dados auditáveis do despacho feito para a OpenAI. */
public record OpenAiDispatch(
        String openAiJobId,
        String prompt,
        String schemaJson,
        String requestBodyJson,
        Instant dispatchedAt
) {
    /** Valida os campos obrigatórios e define o horário do despacho quando ausente. */
    public OpenAiDispatch {
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(schemaJson, "schemaJson must not be null");
        Objects.requireNonNull(requestBodyJson, "requestBodyJson must not be null");
        dispatchedAt = dispatchedAt == null ? Instant.now() : dispatchedAt;
    }
}
