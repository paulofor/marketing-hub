package com.marketinghub.oprmcoletormei.catalog.dto;

import java.time.Instant;

public record CnaeCatalogExecutionLogEntry(
        Instant timestamp,
        String trigger,
        String status,
        String message,
        Integer received,
        Integer normalized,
        Integer persisted
) {
}
