package com.marketinghub.ads.dto;

import java.time.Instant;

public record FacebookInstantFormDto(
        Long id,
        String hypothesisId,
        Long facebookPageId,
        String facebookPageExternalId,
        String facebookPageName,
        String facebookFormId,
        String name,
        String status,
        String locale,
        Long leadsCount,
        Instant createdTime,
        Instant updatedTime,
        String followUpActionUrl,
        String privacyPolicyUrl,
        String model,
        String prompt,
        boolean approved,
        Instant approvedAt,
        boolean published,
        Instant publishedAt,
        String shareLink,
        Instant createdAt,
        Instant updatedAt
) {
}
