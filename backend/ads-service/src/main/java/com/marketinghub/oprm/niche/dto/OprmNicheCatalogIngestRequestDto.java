package com.marketinghub.oprm.niche.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OprmNicheCatalogIngestRequestDto(
        @NotBlank String source,
        @NotEmpty List<@Valid RecordDto> records
) {
    public record RecordDto(
            @NotBlank String cnaeCode,
            @NotBlank String cnaeLabel,
            Boolean active
    ) {
    }
}
