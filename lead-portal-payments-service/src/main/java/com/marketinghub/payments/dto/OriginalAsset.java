package com.marketinghub.payments.dto;

public record OriginalAsset(
        long itemId,
        Integer positionIndex,
        String objectKey,
        String contentType
) {
}
