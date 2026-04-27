package com.marketinghub.experiment.pipeline.dto;

import java.util.List;

public record LandingPagePublicationResultDto(
        Long experimentId,
        Long flowId,
        boolean approved,
        boolean published,
        String publicUrl,
        List<LandingPageVariantLinksDto> variantLinks,
        String facebookPixelId,
        boolean pixelAppliedAutomatically,
        String message) {
}
