package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.List;

public final class MoisHotmartProductDtos {

    private MoisHotmartProductDtos() {}

    public record HotmartCollectedProductResponse(
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
    ) {}

    public record HotmartCollectedProductListResponse(
            String workspaceId,
            List<HotmartCollectedProductResponse> items
    ) {}
}
