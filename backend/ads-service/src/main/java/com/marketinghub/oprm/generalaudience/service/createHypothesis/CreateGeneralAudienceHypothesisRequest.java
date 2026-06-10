package com.marketinghub.oprm.generalaudience.service.createHypothesis;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Contrato de entrada para criar hipótese específica a partir de um ângulo aprovado de público geral. */
public record CreateGeneralAudienceHypothesisRequest(
        @Size(max = 255) String title,
        String successRule,
        @DecimalMin("0.00") BigDecimal kpiTargetCpl
) {
}
