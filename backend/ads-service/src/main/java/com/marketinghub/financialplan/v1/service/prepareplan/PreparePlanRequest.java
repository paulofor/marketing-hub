package com.marketinghub.financialplan.v1.service.prepareplan;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions.MonthlySubscription;
import com.marketinghub.financialplan.v1.service.saveplan.WholeNumberDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Responsabilidade: receber duas escolhas e a identidade do contexto apresentado na tela. */
public record PreparePlanRequest(
    @NotNull @Min(0) Integer expectedRevision,
    @NotNull Long commercialPlanId,
    @NotNull Integer commercialPlanVersion,
    @NotBlank @Size(max = 100) String productVersion,
    @NotNull @Min(1) @Max(3660) @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer supportDays,
    Boolean personalizedAi,
    Long expectedProductTypeId,
    @Valid MonthlySubscription subscription,
    @DecimalMin("0.01") @Digits(integer = 9, fraction = 6) BigDecimal monthlyPriceBrl,
    @Min(0) @Max(10000000) @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer newSubscribers) {
  /** Preserva a entrada anterior de preparação por venda. */
  public PreparePlanRequest(
      Integer expectedRevision,
      Long commercialPlanId,
      Integer commercialPlanVersion,
      String productVersion,
      Integer supportDays,
      Boolean personalizedAi) {
    this(
        expectedRevision,
        commercialPlanId,
        commercialPlanVersion,
        productVersion,
        supportDays,
        personalizedAi,
        null,
        null,
        null,
        null);
  }

  /** Assume geração personalizada quando a escolha ainda não foi registrada. */
  public PreparePlanRequest {
    if (personalizedAi == null) personalizedAi = true;
  }
}
