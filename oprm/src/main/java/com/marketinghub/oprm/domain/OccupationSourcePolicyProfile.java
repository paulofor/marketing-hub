package com.marketinghub.oprm.domain;

import java.util.List;

public record OccupationSourcePolicyProfile(
        List<String> allowedDomains,
        List<String> blockedDomains,
        String rateLimitPolicy,
        String sourceRiskLevel,
        boolean manualReviewRequired,
        String notes) {
}
