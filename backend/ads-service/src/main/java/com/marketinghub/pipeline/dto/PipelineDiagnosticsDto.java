package com.marketinghub.pipeline.dto;

import java.util.List;
import lombok.Builder;

/**
 * DTO que resume o diagnóstico de aderência entre banco e contrato oficial do pipeline.
 */
@Builder
public record PipelineDiagnosticsDto(
        Long pipelineId,
        String pipelineCode,
        String canonicalPipelineCode,
        String status,
        int expectedStages,
        int configuredStages,
        List<PipelineDiagnosticsIssueDto> issues) {}
