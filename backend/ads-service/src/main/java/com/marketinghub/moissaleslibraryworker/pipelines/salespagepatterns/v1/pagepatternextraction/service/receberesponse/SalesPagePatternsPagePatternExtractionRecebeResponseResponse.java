package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse;

/** Contrato de confirmação da resposta persistida na extração de padrões de página. */
public record SalesPagePatternsPagePatternExtractionRecebeResponseResponse(
        String jobId,
        String idExterno,
        String stageCode,
        String status,
        String nextStageCode) {
}
