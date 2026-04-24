package com.marketinghub.mois.dto;

import java.util.List;

public final class MoisOfferDtos {

    private MoisOfferDtos() {
    }

    public record OfferCardSummaryResponse(
            String offerId,
            String requestId,
            String nicheName,
            String offerName,
            String sellerOrBrand,
            String corePromise,
            String primaryOfferType,
            String mainPrice,
            Double confidence
    ) {
    }

    public record OfferCardResponse(
            String offerId,
            String requestId,
            String nicheName,
            String offerName,
            String sellerOrBrand,
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
            List<MoisDiscoveryDtos.ArtifactRefResponse> sourceArtifacts
    ) {
    }

    public record OfferCardListResponse(List<OfferCardSummaryResponse> items) {
    }
}
