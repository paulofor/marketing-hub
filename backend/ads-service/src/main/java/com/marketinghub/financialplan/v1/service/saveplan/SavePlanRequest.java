package com.marketinghub.financialplan.v1.service.saveplan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Responsabilidade: solicitar revisão imutável com prevenção de edição concorrente. */
public record SavePlanRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 100) String createdBy,
    @NotNull @Min(0) Integer expectedRevision,
    Long commercialPlanId,
    Long templateId,
    @NotNull @Valid PlanAssumptions assumptions) {}
