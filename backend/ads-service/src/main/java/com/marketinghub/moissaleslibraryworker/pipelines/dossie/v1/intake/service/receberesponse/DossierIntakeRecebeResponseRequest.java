package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa intake do dossiê MOIS v1. */
public record DossierIntakeRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
