package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de auditoria da resposta funcional da extração de padrões de página. */
public record SalesPagePatternsPagePatternExtractionRecebeResponseRequest(
        String response,
        Integer quantidadeTokenEntrada,
        Integer quantidadeTokenSaida,
        BigDecimal custo,
        String modelo,
        String descricaoErro) {
}
