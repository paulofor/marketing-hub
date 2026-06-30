package com.marketinghub.worker.pipeline.gerasalespagev1;

import java.util.Map;

/** Responsabilidade: transportar entrada, template e contexto de uma etapa do GeraSalesPage v1. */
public record GeraSalesPageInput(
        Long experimentId,
        String stageCode,
        String idJob,
        String model,
        String schemaName,
        String promptMarkdownContent,
        String schemaJson,
        Map<String, Object> promptData
) {
    /** Normaliza dados opcionais para simplificar o processor da etapa. */
    public GeraSalesPageInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
