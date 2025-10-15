package com.marketinghub.facebookadsworker.facebookinstantform;

import java.time.Instant;

public record InstantFormPublicationUpdateRequest(
        boolean published,
        Instant publishedAt,
        String shareLink,
        String status,
        String facebookFormId
) {
}
