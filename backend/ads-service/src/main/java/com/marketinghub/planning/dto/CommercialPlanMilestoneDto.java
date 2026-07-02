package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanMilestoneStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Responsabilidade: expor um marco comercial do plano para a interface. */
public record CommercialPlanMilestoneDto(
        Long id,
        Integer sequenceOrder,
        String code,
        String name,
        CommercialPlanMilestoneStatus status,
        LocalDate dueDate,
        BigDecimal targetCost,
        BigDecimal targetRevenue,
        Integer experimentsToCreate,
        Integer experimentsToPublish,
        BigDecimal actualCampaignCost,
        BigDecimal actualAiCost,
        BigDecimal actualTotalCost,
        BigDecimal actualRevenue,
        Integer actualExperimentsCreated,
        Integer actualExperimentsPublished,
        java.time.Instant executionSyncedAt,
        String evidenceSource,
        String blocker,
        String recommendedNextAction) {}
