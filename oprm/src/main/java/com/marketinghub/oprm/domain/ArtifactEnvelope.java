package com.marketinghub.oprm.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ArtifactEnvelope(
        String artifactType,
        String artifactVersion,
        String artifactId,
        String moduleName,
        String producer,
        Instant createdAt,
        String correlationId,
        String traceId,
        List<String> sourceRefs,
        List<String> inputRefs,
        OccupationProfileSnapshotPayload payload,
        String status,
        double confidenceScore,
        Map<String, Object> metadata) {
}
