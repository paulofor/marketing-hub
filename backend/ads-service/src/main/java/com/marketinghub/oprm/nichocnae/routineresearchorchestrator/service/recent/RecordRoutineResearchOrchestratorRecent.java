package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa um nicho recentemente processado pela etapa zero do pipeline OPRM nichocnae. */
public record RecordRoutineResearchOrchestratorRecent(
        Long researchCycleId,
        Long sourceNicheId,
        Long existingMarketNicheId,
        Boolean alreadyMaterialized,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        String originalNicheName,
        String neutralNicheName,
        String researchMode,
        BigDecimal solutionLanguageRiskScore,
        BigDecimal sourceScore,
        String audienceName,
        Integer autonomousProfessionalFitScore,
        Integer sourceFreshnessScore,
        Integer outdatedSourceRiskScore,
        Integer structuredBusinessDriftRiskScore,
        String gateStatus,
        String triggerSource,
        String cycleStatus,
        Instant processedAt,
        Instant finishedAt,
        String errorMessage) {}
