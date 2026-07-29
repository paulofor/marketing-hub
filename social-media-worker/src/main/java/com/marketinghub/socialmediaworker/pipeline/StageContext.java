package com.marketinghub.socialmediaworker.pipeline;

import java.time.Instant;
import java.util.Map;

/**
 * Carrega metadados auditaveis da execucao de uma etapa do pipeline.
 */
public record StageContext(String stageCode, Long executionId, String jobId, Instant receivedAt, Map<String, Object> metadata) {

    /**
     * Cria um contexto minimo para uma execucao recebida do backend.
     */
    public static StageContext of(String stageCode, Long executionId, String jobId) {
        return new StageContext(stageCode, executionId, jobId, Instant.now(), Map.of());
    }
}
