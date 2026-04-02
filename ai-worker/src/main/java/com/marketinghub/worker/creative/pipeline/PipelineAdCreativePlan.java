package com.marketinghub.worker.creative.pipeline;

/**
 * Represents the pairing of an ad copy variant with its corresponding image briefing.
 */
public record PipelineAdCreativePlan(
        String variantKey,
        String placementHint,
        String headline,
        String primaryText,
        String description,
        String ctaText,
        String format,
        PipelineImageBriefing imageBriefing) {
}
