package com.marketinghub.experiment.pipeline.dto;

public record LandingPagePublicationResultDto(
        Long experimentId,
        Long flowId,
        boolean approved,
        boolean published,
        String publicUrl,
        String facebookPixelId,
        boolean pixelAppliedAutomatically,
        String message) {
}
