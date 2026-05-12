package com.marketinghub.settings.dto;

import java.time.Instant;

public record HotmartCollectedProductDto(
        String jobId,
        String referenceId,
        String title,
        String productUrl,
        String producerName,
        String imageUrl,
        Integer successScore,
        String price,
        String currency,
        Instant collectedAt
) {
}
