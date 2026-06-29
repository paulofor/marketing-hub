package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanRecommendation;
import java.time.Instant;

/** Responsabilidade: expor uma simulacao de cenario do plano para a interface. */
public record CommercialPlanSimulationDto(
        Long id,
        CommercialPlanRecommendation recommendation,
        String mostLikelyScenario,
        String bestRealisticScenario,
        String worstLikelyScenario,
        String mainRisk,
        String bestNextAction,
        String actionToAvoid,
        String continueCondition,
        String stopCondition,
        String evidence7Days,
        String evidence14Days,
        String evidence30Days,
        String decisionNotes,
        Instant createdAt) {}
