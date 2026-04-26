package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MoisCollectionPersistenceDtos {

    private MoisCollectionPersistenceDtos() {
    }

    public record CollectionJobStateResponse(
            MoisWorkspaceDtos.CollectionJobResponse job,
            List<MoisWorkspaceDtos.CollectedReferenceResponse> references,
            Map<String, MoisWorkspaceDtos.CollectedReferenceLineageResponse> lineageByReferenceId,
            RuntimeStatsResponse runtime,
            List<MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse> sourceOps
    ) {
    }

    public record RuntimeStatsResponse(
            int retries,
            long latencyMs,
            Instant finishedAt
    ) {
    }

    public record CollectionJobStateListResponse(List<CollectionJobStateResponse> items) {
    }
}
