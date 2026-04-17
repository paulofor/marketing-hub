package com.marketinghub.mds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MdsSourceAccessPublishBatchRequest(
        @NotEmpty List<@Valid SourceAccessPayload> records
) {
    public record SourceAccessPayload(
            @NotBlank String sourceDocumentId,
            @NotBlank String accessClass,
            @NotBlank String permissionState,
            String licenseText,
            String accessUrl
    ) {
    }
}
