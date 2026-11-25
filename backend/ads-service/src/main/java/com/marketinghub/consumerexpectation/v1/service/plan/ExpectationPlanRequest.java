package com.marketinghub.consumerexpectation.v1.service.plan;

import jakarta.validation.constraints.*;

/** Declara contexto e evidências para preparar hipóteses, sem registrar aprovação ou gasto. */
public record ExpectationPlanRequest(
    @NotBlank @Size(max = 500) String audience,
    @NotBlank @Size(max = 500) String context,
    @NotNull Surface surface,
    @NotBlank @Size(max = 1000) String expectedExperience,
    @NotBlank @Size(max = 1000) String desiredOutcome,
    @NotBlank @Size(max = 1000) String actualDelivery,
    @NotNull EvidenceBasis evidenceBasis,
    @Size(max = 1200) String evidenceReference,
    @Size(max = 1200) String deliveryProofReference) {
  /** Separa as superfícies nas quais uma única mudança será formulada. */
  public enum Surface {
    AD,
    OFFER,
    PRODUCT,
    DIGITAL_EXPERIENCE
  }

  /** Distingue expectativa inferida de observação pública ou comportamento voluntário. */
  public enum EvidenceBasis {
    HYPOTHESIS,
    PUBLIC_SOURCE,
    VOLUNTARY_BEHAVIOR
  }
}
