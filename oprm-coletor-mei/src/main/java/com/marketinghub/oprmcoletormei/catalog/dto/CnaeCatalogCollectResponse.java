package com.marketinghub.oprmcoletormei.catalog.dto;

public record CnaeCatalogCollectResponse(
        int received,
        int normalized,
        int deduplicated,
        int batchesSent,
        int persisted
) {
}
