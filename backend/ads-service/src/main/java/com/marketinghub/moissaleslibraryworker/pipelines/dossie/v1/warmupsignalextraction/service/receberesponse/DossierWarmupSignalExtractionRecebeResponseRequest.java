package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.receberesponse;

import java.math.BigDecimal;

/** Contrato de entrada do endpoint recebeResponse da etapa warmupsignalextraction do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionRecebeResponseRequest(
        String response,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {
}
