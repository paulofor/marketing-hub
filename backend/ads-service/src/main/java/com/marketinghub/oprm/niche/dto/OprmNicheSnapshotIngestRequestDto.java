package com.marketinghub.oprm.niche.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;

public record OprmNicheSnapshotIngestRequestDto(
        @NotNull LocalDate snapshotDate,
        @NotBlank String source,
        @NotEmpty List<@Valid RecordDto> records
) {
    public record RecordDto(
            @NotBlank String cnaeCode,
            @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "uf deve conter 2 letras maiúsculas") String uf,
            @NotBlank String municipio,
            @NotNull @PositiveOrZero Integer meiActive,
            @NotNull @PositiveOrZero Integer openings,
            @NotNull @PositiveOrZero Integer closures,
            @NotNull Integer net
    ) {
    }
}
