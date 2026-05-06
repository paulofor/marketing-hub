package com.marketinghub.moishotmart.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public final class HotmartDtos {

    private HotmartDtos() {
    }

    public record HotmartCollectionRequest(
            @NotBlank String source,
            int maxProducts
    ) {
    }

    public record HotmartProductSnapshot(
            String title,
            String rating,
            String commission,
            String detailsUrl,
            Instant collectedAt
    ) {
    }

    public record HotmartCollectionResponse(
            String status,
            String message,
            List<HotmartProductSnapshot> products
    ) {
    }
}
