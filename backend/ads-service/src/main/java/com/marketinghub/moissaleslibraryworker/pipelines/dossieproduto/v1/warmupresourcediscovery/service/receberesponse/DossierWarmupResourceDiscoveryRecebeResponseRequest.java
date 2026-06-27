package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa warmupresourcediscovery do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
