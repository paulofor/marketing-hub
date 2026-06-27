package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa investigationanchorbuilder do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
