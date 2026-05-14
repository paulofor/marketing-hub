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
            String price,
            String currency,
            String salesPageUrl,
            Double temperature,
            Instant collectedAt
    ) {}

    public record HotmartCollectedProductListResponse(
            String workspaceId,
            List<HotmartCollectedProductResponse> items
    ) {}
}
