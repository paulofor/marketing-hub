package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending;

import java.time.Instant;
import java.util.List;

/** Contrato interno que entrega evidências coletadas para a IA segmentar público MEI/autônomo sem criar produto. */
public record RecordMeiAudienceSegmenterPending(
    Long researchCycleId,
    Long routineCardId,
    Long sourceNicheId,
    String cnaeCode,
    String cnaeDescription,
    String neutralNicheName,
    String nicheName,
    String routineSummary,
    String painsSummary,
    String resultsSummary,
    String evidenceSummary,
    String sourceDomains,
    Integer routineEvidenceScore,
    Integer difficultyEvidenceScore,
    Integer sourceDiversityScore,
    Integer solutionLanguageRiskScore,
    Instant routineCardCreatedAt,
    List<SegmenterSourceSnapshotResponse> sources,
    List<SegmenterSignalResponse> signals) {}
