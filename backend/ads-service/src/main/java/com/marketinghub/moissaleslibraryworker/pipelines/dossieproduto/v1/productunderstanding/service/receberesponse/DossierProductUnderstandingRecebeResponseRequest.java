package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa productunderstanding do dossiê MOIS v1. */
public record DossierProductUnderstandingRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo,
        String promptTemplateKey,
        String promptTemplateVersion,
        String schemaName) {
}
