package com.marketinghub.ads.dto;

import java.time.Instant;

public record UpdateFacebookInstantFormPublicationRequest(
        boolean published,
        Instant publishedAt,
        String shareLink,
        String status,
        String facebookFormId
) {
}
