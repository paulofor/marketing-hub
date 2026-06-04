package com.marketinghub.pipeline.dto;

import lombok.Builder;

/**
 * DTO que descreve uma divergência específica encontrada no contrato operacional do pipeline.
 */
@Builder
public record PipelineDiagnosticsIssueDto(
        String severity,
        String stageCode,
        String canonicalCode,
        String message,
        String rootCause,
        String recommendedAction) {}
