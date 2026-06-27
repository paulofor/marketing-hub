package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa sourceproductmatch do dossiê MOIS v1. */
public record DossierSourceProductMatchRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
