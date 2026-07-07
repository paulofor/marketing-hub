package com.marketinghub.planning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Responsabilidade: expor o consolidado semanal de experimentos do planejamento comercial. */
public record CommercialPlanWeekDto(
        Integer weekNumber,
        LocalDate startDate,
        LocalDate endDate,
        Integer experimentsCreated,
        BigDecimal totalCost,
        BigDecimal totalRevenue,
        List<CommercialPlanWeekObjectiveDto> objectives,
        List<CommercialPlanWeekExperimentDto> experiments) {}
