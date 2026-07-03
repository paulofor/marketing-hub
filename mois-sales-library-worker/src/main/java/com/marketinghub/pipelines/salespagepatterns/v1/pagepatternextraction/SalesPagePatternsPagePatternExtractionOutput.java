package com.marketinghub.pipelines.salespagepatterns.v1.pagepatternextraction;

import java.util.Map;

/** Saída funcional da extração de padrões vencedores de página de venda. */
public record SalesPagePatternsPagePatternExtractionOutput(
        long pageId,
        String status,
        String patternsJson,
        Map<String, Object> evidence) {
}
