package com.marketinghub.financialagent.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Responsabilidade: receber a decisão comercial auditável sobre uma cena produzida. */
public record CommercialSceneEvaluationRequest(
    @NotBlank String status,
    @NotNull @Min(0) @Max(100) Integer utilizationPercent,
    String notes,
    @NotBlank String evaluatedBy) {}
