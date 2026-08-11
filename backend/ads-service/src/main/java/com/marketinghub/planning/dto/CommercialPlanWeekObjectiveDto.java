package com.marketinghub.planning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Responsabilidade: expor um objetivo editavel da próxima semana no planejamento comercial. */
public record CommercialPlanWeekObjectiveDto(
    Long id,
    Integer sequenceOrder,
    String objectiveText,
    Integer score,
    Integer planVersionNumber,
    String assignedAgentKey,
    String assignedAgentNickname,
    String expectedResult,
    String executionStatus,
    LocalDate dueDate,
    BigDecimal plannedCost,
    BigDecimal plannedRevenue) {}
