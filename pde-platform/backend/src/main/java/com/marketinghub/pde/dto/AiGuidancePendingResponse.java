package com.marketinghub.pde.dto;

import java.util.Map;

/** Entrega ao worker PDE uma pendência de IA com contexto suficiente para execução. */
public record AiGuidancePendingResponse(
        String requestId,
        String productSlug,
        String email,
        String missionId,
        String guidanceType,
        String stageCode,
        String status,
        Map<String, String> answers,
        Map<String, String> previousMissionAnswers,
        ProductExperienceResponse product
) {}
