package com.marketinghub.scientificresearch.productevidence.v1.sourcediscovery;

import java.util.List;

/**
 * Agrupa as fontes científicas aprovadas para a etapa de síntese.
 */
public record SourceDiscoveryOutput(String query, List<ScientificSource> sources, List<String> rejectedReasons) {
}
