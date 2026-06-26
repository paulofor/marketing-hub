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
        Map<String, Object> metadata,
        String serviceTier
) {
    /** Mantém compatibilidade com etapas existentes que usam Flex por padrão. */
    public OpenAiRequest(
            String model,
            String prompt,
            String requestBodyJson,
            String schemaName,
            String schemaJson,
            String promptMarkdownContent,
            Map<String, Object> metadata
    ) {
        this(model, prompt, requestBodyJson, schemaName, schemaJson, promptMarkdownContent, metadata, "flex");
    }

    /** Valida os campos obrigatórios e normaliza metadados opcionais. */
    public OpenAiRequest {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(requestBodyJson, "requestBodyJson must not be null");
        Objects.requireNonNull(promptMarkdownContent, "promptMarkdownContent must not be null");
        serviceTier = normalizeServiceTier(serviceTier);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Normaliza o tier OpenAI preservando Flex como padrão seguro das etapas legadas. */
    private static String normalizeServiceTier(String value) {
        if (value == null || value.isBlank()) {
            return "flex";
        }
        String normalized = value.trim().toLowerCase();
        return "standard".equals(normalized) ? "default" : normalized;
    }
}
