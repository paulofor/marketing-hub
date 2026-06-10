package com.marketinghub.oprm.generalaudience.service.createLeadExperiment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Contrato de entrada para criar experimento curto de lead/isca a partir de público geral aprovado. */
public record CreateGeneralAudienceLeadExperimentRequest(
        @NotNull UUID hypothesisId,
        @Size(max = 255) String name,
        @Size(max = 191) String primaryMetric,
        @NotNull @DecimalMin("0.01") BigDecimal stopLossCpl,
        @NotNull @DecimalMin("0.01") BigDecimal dailyBudget,
        @NotNull @Min(1) @Max(14) Integer durationDays,
        @DecimalMin("0.01") BigDecimal kpiTargetCpl,
        @Min(1) Integer sampleSize
) {
}
