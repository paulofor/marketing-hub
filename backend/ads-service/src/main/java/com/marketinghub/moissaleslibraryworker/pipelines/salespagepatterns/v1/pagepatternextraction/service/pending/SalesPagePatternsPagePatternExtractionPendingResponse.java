package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending;

import java.util.List;

/** Contrato de resposta com os trabalhos de extração de padrões reservados pelo backend. */
public record SalesPagePatternsPagePatternExtractionPendingResponse(
        boolean claimed,
        List<SalesPagePatternsPagePatternExtractionPendingJob> jobs) {
}
