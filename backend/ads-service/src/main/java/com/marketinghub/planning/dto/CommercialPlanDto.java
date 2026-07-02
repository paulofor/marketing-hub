package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.CommercialPlanType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Responsabilidade: expor o plano comercial completo para consumo da interface. */
public record CommercialPlanDto(
        Long id,
        String name,
        CommercialPlanType planType,
        CommercialPlanStatus status,
        Long nicheId,
        String nicheName,
        UUID hypothesisId,
        String hypothesisTitle,
        Long experimentId,
        String experimentName,
        String commercialObjective,
        String targetAudience,
        String mainPain,
        String mainOffer,
        String mainLeadMagnet,
        String mainChannel,
        String mainMetric,
        String successCriteria,
        String stopCriteria,
        LocalDate deadline,
        BigDecimal maxBudget,
        BigDecimal targetRevenue,
        BigDecimal operationalRevenueTarget,
        Integer experimentsToCreate,
        Integer experimentsToPublish,
        long daysRemaining,
        String nextAction,
        String currentBlocker,
        String rootCause,
        String mostLikelyScenario,
        String mainFutureRisk,
        String actionToAvoid,
        List<CommercialPlanMilestoneDto> milestones,
        List<CommercialPlanSimulationDto> simulations,
        Instant createdAt,
        Instant updatedAt) {}
