package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.List;

public final class MoisClickbaseProductDtos {

    private MoisClickbaseProductDtos() {}

    public record ClickbaseCollectedProductResponse(
            String jobId,
            String referenceId,
            String title,
            String productUrl,
            String producerName,
            Integer successScore,
            Instant collectedAt
    ) {}

    public record ClickbaseCollectedProductListResponse(
            String workspaceId,
            List<ClickbaseCollectedProductResponse> items
    ) {}
}
