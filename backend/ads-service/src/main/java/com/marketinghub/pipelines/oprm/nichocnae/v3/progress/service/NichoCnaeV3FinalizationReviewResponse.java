package com.marketinghub.pipelines.oprm.nichocnae.v3.progress.service;

/** Representa a conferência manual exigida antes da etapa final do pipeline NichoCNAE v3. */
public record NichoCnaeV3FinalizationReviewResponse(
        boolean requiresConfirmation,
        Long qualityGateStageExecutionId,
        String materializationMode,
        Long targetMarketNicheId,
        String targetNicheName,
        String nicheInformation,
        String enrichedNicheInformation) {}
