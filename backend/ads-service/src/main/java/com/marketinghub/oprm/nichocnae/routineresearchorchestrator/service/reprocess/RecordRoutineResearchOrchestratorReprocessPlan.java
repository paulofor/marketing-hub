package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess;

import java.util.List;

/** Representa o plano mínimo de rewind para reprocessar um job sem perder evidências aceitas. */
public record RecordRoutineResearchOrchestratorReprocessPlan(
        Long researchCycleId,
        String rewindStageCode,
        String knowledgeVersion,
        boolean preserveAcceptedEvidence,
        List<Long> preservedSourceSnapshotIds,
        List<Long> preservedSignalIds,
        List<Long> rejectedSourceSnapshotIds,
        List<String> evidenceGaps,
        String reasonCode) {}
