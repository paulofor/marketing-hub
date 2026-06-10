package com.marketinghub.oprm.generalaudience.service.createLeadExperiment;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato de saída do experimento de lead/isca criado para público geral. */
public record GeneralAudienceLeadExperimentResponse(
        Long painAngleId,
        Long subnicheId,
        Long marketNicheId,
        Long experimentId,
        String experimentName,
        String status,
        String primaryMetric,
        BigDecimal stopLossCpl,
        BigDecimal dailyBudget,
        LocalDate startDate,
        LocalDate endDate
) {
}
