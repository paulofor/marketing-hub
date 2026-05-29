package com.marketinghub.worker.geralanding.wireframe.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responsável por representar os detalhes retornados pelo backend para uma execução de etapa do GeraLanding.
 */
public record GeraLandingStageExecutionDetailDto(
        Long experimentId,
        String stageCode,
        String idJob,
        String status,
        Instant executionRequestedAt,
        Instant processingStartedAt,
        Instant completedAt,
        String openAiJobId,
        Map<String, Object> promptData) {

    /** Mantém compatibilidade com respostas antigas do backend que não enviam dados de prompt embutidos. */
    public GeraLandingStageExecutionDetailDto(
            Long experimentId,
            String stageCode,
            String idJob,
            String status,
            Instant executionRequestedAt,
            Instant processingStartedAt,
            Instant completedAt,
            String openAiJobId) {
        this(experimentId, stageCode, idJob, status, executionRequestedAt, processingStartedAt, completedAt, openAiJobId, Map.of());
    }

    /** Normaliza dados de prompt nulos para um mapa seguro e imutável. */
    public GeraLandingStageExecutionDetailDto {
        promptData = promptData != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(promptData))
                : Map.of();
    }
}
