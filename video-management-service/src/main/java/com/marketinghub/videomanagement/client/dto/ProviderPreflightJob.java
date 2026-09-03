package com.marketinghub.videomanagement.client.dto;

import java.math.BigDecimal;

/** Responsabilidade: representar um ciclo aguardando preflight oficial no executor de vídeo. */
public record ProviderPreflightJob(
        Long preflightId,
        Long cycleId,
        String aggregatorName,
        String accountKey,
        String productionProfile,
        BigDecimal maxCredits,
        Integer targetDurationSeconds,
        Integer providerClipDurationSeconds,
        Integer generationClipCount,
        String aspectRatio,
        String resolution,
        boolean audio,
        String title,
        String objective,
        String hookText,
        String scriptText,
        String scenePlan,
        String characterBible,
        String environmentBible,
        String visualStyleGuide,
        String continuityRules,
        String learningObjective,
        String successCriterion) {}
