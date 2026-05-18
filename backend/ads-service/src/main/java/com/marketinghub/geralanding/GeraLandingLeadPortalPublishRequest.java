package com.marketinghub.geralanding;

public record GeraLandingLeadPortalPublishRequest(
        String slug,
        String name,
        String description,
        String customFormHtml,
        String legacyPreviewHtml,
        String renderMode) {
}
