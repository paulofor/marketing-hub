package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.time.Instant;
import java.util.List;

/** Unidade de trabalho da etapa de segmentação comportamental MEI/autônomo. */
public record MeiAudienceSegmenterPending(
        Long researchCycleId,
        Long routineCardId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String neutralNicheName,
        String nicheName,
        String openAiModelCode,
        String openAiModelName,
        String routineSummary,
        String customerBehaviorSummary,
        String channelsSummary,
        String operationalPainsSummary,
        String emotionalPainsSummary,
        String dreamsSummary,
        String fearsSummary,
        String languageSummary,
        String painsSummary,
        String resultsSummary,
        String evidenceSummary,
        String sourceDomains,
        Integer routineEvidenceScore,
        Integer difficultyEvidenceScore,
        Integer sourceDiversityScore,
        Integer solutionLanguageRiskScore,
        Instant routineCardCreatedAt,
        List<SegmenterSourceSnapshot> sources,
        List<SegmenterSignal> signals) {}
