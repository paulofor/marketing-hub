package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa dossiersynthesis do dossiê MOIS v1. */
public record DossierDossierSynthesisRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
