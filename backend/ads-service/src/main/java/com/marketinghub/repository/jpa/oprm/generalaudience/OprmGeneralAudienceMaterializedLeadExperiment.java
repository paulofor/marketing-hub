package com.marketinghub.repository.jpa.oprm.generalaudience;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Referência do experimento de lead criado para manter o OPRM desacoplado do domínio de experimentos. */
public record OprmGeneralAudienceMaterializedLeadExperiment(
        Long id,
        String name,
        String status,
        String primaryMetric,
        BigDecimal stopLossCpl,
        BigDecimal dailyBudget,
        LocalDate startDate,
        LocalDate endDate
) {
}
