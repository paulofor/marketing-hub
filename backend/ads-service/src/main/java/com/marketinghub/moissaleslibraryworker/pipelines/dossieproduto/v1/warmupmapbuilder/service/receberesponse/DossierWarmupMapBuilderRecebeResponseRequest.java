package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupmapbuilder.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa warmupmapbuilder do dossiê MOIS v1. */
public record DossierWarmupMapBuilderRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
