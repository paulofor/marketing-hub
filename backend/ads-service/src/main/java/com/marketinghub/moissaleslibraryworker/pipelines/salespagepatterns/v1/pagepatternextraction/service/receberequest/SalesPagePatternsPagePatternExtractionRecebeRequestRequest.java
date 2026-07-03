package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberequest;

/** Contrato de auditoria do request enviado pelo worker na extração de padrões de página. */
public record SalesPagePatternsPagePatternExtractionRecebeRequestRequest(
        String request,
        String plataforma,
        String prompt,
        String schema,
        String promptTemplateKey,
        String promptTemplateVersion,
        String schemaName) {
}
