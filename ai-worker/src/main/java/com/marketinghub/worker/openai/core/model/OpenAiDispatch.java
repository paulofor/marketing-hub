package com.marketinghub.worker.openai.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Responsabilidade: transportar os dados auditáveis do despacho feito para a OpenAI. */
public record OpenAiDispatch(
        String openAiJobId,
        String prompt,
        String schemaJson,
        String requestBodyJson,
        String promptMarkdownContent,
        Instant dispatchedAt,
        Map<String, Object> metadata
) {
    /** Valida os campos obrigatórios e define o horário do despacho quando ausente. */
    public OpenAiDispatch {
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(schemaJson, "schemaJson must not be null");
        Objects.requireNonNull(requestBodyJson, "requestBodyJson must not be null");
        Objects.requireNonNull(promptMarkdownContent, "promptMarkdownContent must not be null");
        dispatchedAt = dispatchedAt == null ? Instant.now() : dispatchedAt;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Mantém compatibilidade com despachos que ainda não produzem metadados adicionais. */
    public OpenAiDispatch(
            String openAiJobId,
            String prompt,
            String schemaJson,
            String requestBodyJson,
            String promptMarkdownContent,
            Instant dispatchedAt
    ) {
        this(openAiJobId, prompt, schemaJson, requestBodyJson, promptMarkdownContent, dispatchedAt, Map.of());
    }
}
