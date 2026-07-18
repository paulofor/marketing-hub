package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/** Recebe do worker PDE o resultado auditável de uma execução OpenAI. */
public record AiGuidanceResultRequest(
        @NotBlank String status,
        String headline,
        String summary,
        List<String> signals,
        List<String> microActions,
        String caution,
        String model,
        String serviceTier,
        String rawRequestJson,
        String rawResponseJson,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String errorMessage
) {}
