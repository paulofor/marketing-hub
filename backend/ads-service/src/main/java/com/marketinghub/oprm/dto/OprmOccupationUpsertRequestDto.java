package com.marketinghub.oprm.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record OprmOccupationUpsertRequestDto(
        @NotBlank String occupationSeedRef,
        @NotBlank String displayName,
        List<String> aliases,
        boolean active
) {
}
