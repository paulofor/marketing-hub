package com.marketinghub.experiment.pipeline.dto;

public record LandingPageVariantLinksDto(
        String variant,
        Long flowId,
        String iframeUrl,
        String standaloneUrl) {
}
