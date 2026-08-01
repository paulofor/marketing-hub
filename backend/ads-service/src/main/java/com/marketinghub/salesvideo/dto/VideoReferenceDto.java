package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.VideoReferenceStatus;
import java.time.Instant;

/** Representa um vídeo de referência na fila de análise e aprendizado do estúdio. */
public record VideoReferenceDto(
    Long id,
    String tenantId,
    String title,
    String sourceUrl,
    String sourcePlatform,
    String niche,
    String funnelStage,
    String primaryLearningGoal,
    String successEvidence,
    String analysisNotes,
    VideoReferenceStatus status,
    String createdBy,
    Instant analyzedAt,
    Instant createdAt,
    Instant updatedAt) {}
