package com.marketinghub.mds.dto;

import java.util.List;
import java.util.Map;

public record MdsAdminArtifactItemResponse(
        Long artifactId,
        String artifactType,
        String schemaVersion,
        String version,
        String status,
        List<Long> parentArtifactIds,
        List<Long> childArtifactIds,
        Map<String, Object> content
) {
}
