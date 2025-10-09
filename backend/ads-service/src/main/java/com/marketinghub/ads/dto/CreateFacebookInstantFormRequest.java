package com.marketinghub.ads.dto;

import java.time.Instant;

public record CreateFacebookInstantFormRequest(
        Long facebookPageId,
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
        String prompt
) {
}
