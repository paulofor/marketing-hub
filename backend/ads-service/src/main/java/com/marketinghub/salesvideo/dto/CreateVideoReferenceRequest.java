package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados recebidos da tela para cadastrar um vídeo de referência para análise. */
public record CreateVideoReferenceRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 2048) String sourceUrl,
    @Size(max = 64) String sourcePlatform,
    @Size(max = 191) String niche,
    @Size(max = 64) String funnelStage,
    @NotBlank @Size(max = 1024) String primaryLearningGoal,
    String successEvidence,
    @Size(max = 191) String createdBy) {}
