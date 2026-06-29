package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Responsabilidade: receber alteracoes dos campos comerciais de um plano. */
public record UpdateCommercialPlanRequest(
        String name,
        CommercialPlanStatus status,
        Long nicheId,
        UUID hypothesisId,
        Long experimentId,
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
        String nextAction,
        String currentBlocker,
        String rootCause) {}
