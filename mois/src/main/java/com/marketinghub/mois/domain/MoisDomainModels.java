package com.marketinghub.mois.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MoisDomainModels {

    private MoisDomainModels() {
    }

    public enum DiscoveryStatus {
        DRAFT,
        COLLECTED,
        FAILED
    }

    public record DiscoveryRequest(
            String requestId,
            String nicheName,
            String marketTheme,
            String painOrOutcomeFocus,
            List<String> seedQueries,
            List<String> seedUrls,
            List<String> channels,
            String country,
            String language,
            Map<String, Object> discoveryPolicy,
            DiscoveryStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SourceSnapshot(
            String artifactId,
            String requestId,
            String sourceUrl,
            String canonicalUrl,
            String sourceTitle,
            String sourceKind,
            Instant capturedAt,
            String rawExcerpt
    ) {
    }

    public record OfferCard(
            String artifactId,
            String requestId,
            String nicheName,
            String offerName,
            String sellerOrBrand,
            String canonicalUrl,
            String contentSignature,
            String corePromise,
            String primaryOfferType,
            String mainPrice,
            Double confidence,
            List<String> evidenceRefs,
            List<String> deliverables,
            List<String> pricePoints,
            String proofSummary,
            String mechanismClaimSummary,
            String funnelPatternSummary,
            Instant createdAt
    ) {
    }
}
