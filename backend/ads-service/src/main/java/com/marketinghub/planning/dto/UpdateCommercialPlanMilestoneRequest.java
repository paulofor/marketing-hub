package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanMilestoneStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Responsabilidade: receber a atualizacao de um marco comercial do plano. */
public record UpdateCommercialPlanMilestoneRequest(
        CommercialPlanMilestoneStatus status,
        LocalDate dueDate,
        BigDecimal targetCost,
        BigDecimal targetRevenue,
        Integer experimentsToCreate,
        Integer experimentsToPublish,
        String evidenceSource,
        String blocker,
        String recommendedNextAction) {}
