package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados estruturados para registrar análise comercial de um vídeo de referência. */
public record AnalyzeVideoReferenceRequest(
    @NotBlank String evidence,
    @NotBlank String commercialDiagnosis,
    @NotBlank String sequenceAnalysis,
    @NotBlank String systemLearnings,
    @NotBlank String salesImprovements,
    @NotBlank String operationalDecision,
    @NotBlank String analyzedBy) {}
