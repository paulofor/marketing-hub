package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Recebe a solicitação de orientação por IA para uma missão específica do PDE. */
public record AiGuidanceCreateRequest(
        @NotBlank String guidanceType,
        @NotEmpty Map<String, String> answers
) {}
