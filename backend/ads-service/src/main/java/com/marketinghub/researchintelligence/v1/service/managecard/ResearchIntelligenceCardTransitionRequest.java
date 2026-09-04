package com.marketinghub.researchintelligence.v1.service.managecard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registra a justificativa humana de uma transição editorial. */
public record ResearchIntelligenceCardTransitionRequest(@NotBlank @Size(max = 500) String reason) {}
