package com.marketinghub.planning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Responsabilidade: receber os dados necessarios para criar um plano comercial. */
public record CreateCommercialPlanRequest(
        String name,
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
