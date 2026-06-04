package com.marketinghub.pipeline.dto;

import lombok.Builder;

/**
 * DTO que explicita quais campos de pipeline são estruturais ou operacionais.
 */
@Builder
public record PipelineFieldPolicyDto(
        boolean codeStructural,
        boolean moduleStructural,
        boolean nameStructural,
        boolean descriptionOperational,
        boolean activeOperational) {}
