package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberequest;

/** Contrato de confirmação do request auditado na extração de padrões de página. */
public record SalesPagePatternsPagePatternExtractionRecebeRequestResponse(
        String jobId,
        String idExterno,
        String stageCode,
        String status) {
}
