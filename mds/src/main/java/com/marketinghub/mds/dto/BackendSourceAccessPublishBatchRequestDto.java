package com.marketinghub.mds.dto;

import java.util.List;

public record BackendSourceAccessPublishBatchRequestDto(
        List<SourceAccessPayloadDto> records
) {
    public record SourceAccessPayloadDto(
            String sourceDocumentId,
            String accessClass,
            String permissionState,
            String licenseText,
            String accessUrl
    ) {
    }
}
