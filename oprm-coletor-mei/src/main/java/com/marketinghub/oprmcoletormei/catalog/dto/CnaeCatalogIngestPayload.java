package com.marketinghub.oprmcoletormei.catalog.dto;

import java.util.List;

public record CnaeCatalogIngestPayload(
        String source,
        List<Record> records
) {
    public record Record(
            String cnaeCode,
            String cnaeLabel,
            Boolean active
    ) {}
}
