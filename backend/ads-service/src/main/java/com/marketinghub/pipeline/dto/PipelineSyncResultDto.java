package com.marketinghub.pipeline.dto;

import java.util.List;
import lombok.Builder;

/**
 * DTO que resume uma sincronização segura entre contrato oficial e banco operacional.
 */
@Builder
public record PipelineSyncResultDto(
        String status,
        boolean synchronizedSafely,
        Long pipelineId,
        String pipelineCode,
        String canonicalPipelineCode,
        int expectedStages,
        int configuredStages,
        List<String> appliedActions,
        List<PipelineDiagnosticsIssueDto> issues) {}
