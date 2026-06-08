package com.marketinghub.worker.pipeline;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Responsabilidade: transportar os dados de uma execução de etapa independente de tecnologia concreta. */
public record StageExecution<I>(
        String idJob,
        Long aggregateId,
        String stageCode,
        String status,
        Instant executionRequestedAt,
        I input,
        Map<String, Object> config
) {
    /** Valida campos obrigatórios e normaliza configuração opcional da execução. */
    public StageExecution {
        Objects.requireNonNull(idJob, "idJob must not be null");
        Objects.requireNonNull(stageCode, "stageCode must not be null");
        Objects.requireNonNull(input, "input must not be null");
        config = config == null ? Map.of() : Map.copyOf(config);
    }
}
