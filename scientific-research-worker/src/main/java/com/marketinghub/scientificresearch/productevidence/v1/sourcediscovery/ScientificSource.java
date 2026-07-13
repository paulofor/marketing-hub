package com.marketinghub.scientificresearch.productevidence.v1.sourcediscovery;

/**
 * Representa uma fonte científica candidata encontrada na internet.
 */
public record ScientificSource(
        String title,
        String url,
        String publication,
        String year,
        String sourceType,
        String evidenceQuality,
        String reasonToUse) {
}
