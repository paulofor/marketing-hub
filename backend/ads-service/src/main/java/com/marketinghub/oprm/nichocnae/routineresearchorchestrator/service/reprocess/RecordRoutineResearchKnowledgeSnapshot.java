package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess;

import java.util.List;

/** Representa o snapshot mínimo de conhecimento aceito antes de reprocessar o mesmo job. */
public record RecordRoutineResearchKnowledgeSnapshot(
        Long researchCycleId,
        String knowledgeVersion,
        int sourceSnapshotCount,
        int extractedSignalCount,
        List<Long> acceptedSourceSnapshotIds,
        List<Long> acceptedSignalIds,
        List<Long> rejectedSourceSnapshotIds,
        List<String> evidenceGaps) {}
