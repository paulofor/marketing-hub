package com.marketinghub.salesvideo.referenceanalysis.v1.service.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.salesvideo.VideoReferenceAnalysisStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Visão pública auditável da execução usada pela tela e pelos callbacks. */
public record VideoReferenceAnalysisResponse(
    Long executionId,
    Long referenceId,
    int attemptNumber,
    VideoReferenceAnalysisStatus status,
    JsonNode input,
    JsonNode output,
    JsonNode artifacts,
    String model,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal costUsd,
    String decision,
    String error,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt,
    Instant updatedAt) {}
