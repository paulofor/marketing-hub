package com.marketinghub.financialplan.v1.service.prepareplan;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketinghub.financialplan.v1.service.saveplan.WholeNumberDeserializer;
import jakarta.validation.constraints.*;

/** Responsabilidade: receber duas escolhas e a identidade do contexto apresentado na tela. */
public record PreparePlanRequest(
    @NotNull @Min(0) Integer expectedRevision,
    @NotNull Long commercialPlanId,
    @NotNull Integer commercialPlanVersion,
    @NotBlank @Size(max = 100) String productVersion,
    @NotNull @Min(1) @Max(3660) @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer supportDays,
    Boolean personalizedAi) {
  /** Assume geração personalizada quando a escolha ainda não foi registrada. */
  public PreparePlanRequest {
    if (personalizedAi == null) personalizedAi = true;
  }
}
