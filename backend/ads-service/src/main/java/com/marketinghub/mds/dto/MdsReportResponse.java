package com.marketinghub.mds.dto;

import java.util.Map;

public record MdsReportResponse(
        Long requestId,
        Long artifactId,
        String artifactType,
        String schemaVersion,
        String version,
        String status,
        Map<String, Object> content
) {
}
