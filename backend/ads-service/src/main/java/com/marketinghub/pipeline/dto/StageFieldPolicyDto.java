package com.marketinghub.pipeline.dto;

import lombok.Builder;

/**
 * DTO que explicita quais campos de etapa são estruturais ou operacionais.
 */
@Builder
public record StageFieldPolicyDto(
        boolean codeStructural,
        boolean positionStructural,
        boolean nameStructural,
        boolean requiredStructural,
        boolean descriptionOperational,
        boolean activeOperational,
        boolean openAiModelOperational) {}
