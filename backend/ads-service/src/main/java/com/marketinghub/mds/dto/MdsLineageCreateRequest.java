package com.marketinghub.mds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MdsLineageCreateRequest(
        @NotNull Long parentArtifactId,
        @NotNull Long childArtifactId,
        @NotBlank String relationType
) {
}
