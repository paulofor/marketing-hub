package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending;

/** Contrato de solicitação de pendências da etapa de extração de padrões de página. */
public record SalesPagePatternsPagePatternExtractionPendingRequest(String workspaceId, String workerId, Integer limit) {
}
