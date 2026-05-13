package com.marketinghub.moisclickbank.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public final class ClickbankDtos {

    private ClickbankDtos() {
    }

    public record ClickbankCollectionRequest(
            @NotBlank String source,
            int maxProducts
    ) {
    }

    public record ClickbankProductSnapshot(
            String title,
            String rating,
            String commission,
            String detailsUrl,
            Double temperature,
            String salesPageUrl,
            Instant collectedAt
    ) {
    }

    public record ClickbankCollectionResponse(
            String status,
            String message,
            List<ClickbankProductSnapshot> products
    ) {
    }
}
