package com.marketinghub.oprmcoletormei.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CnaeCatalogCollectRequest(
        @NotBlank String source,
        @NotEmpty List<@Valid RawRecord> records
) {
    public record RawRecord(
            @NotBlank String cnaeCode,
            @NotBlank String cnaeLabel,
            Boolean active
    ) {}
}
