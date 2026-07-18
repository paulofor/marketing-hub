package com.marketinghub.pde.dto;

import java.math.BigDecimal;
import java.util.List;

/** Retorna à cliente o estado e a orientação funcional gerada pela IA. */
public record AiGuidanceResponse(
        String requestId,
        String productSlug,
        String missionId,
        String guidanceType,
        String status,
        String headline,
        String summary,
        List<String> signals,
        List<String> microActions,
        String caution,
        String model,
        String serviceTier,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String errorMessage
) {}
