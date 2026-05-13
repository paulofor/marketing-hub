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
            String ucode,
            String title,
            String image,
            String rating,
            Integer totalAnswers,
            Double blueprint,
            String commission,
            Double priceValue,
            String category,
            String format,
            String producerName,
            String detailsUrl,
            Double temperature,
            String salesPageUrl,
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
