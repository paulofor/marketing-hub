package com.marketinghub.pipelines.dossie.v1.sourceproductmatch;

/** Representa a saída funcional da etapa validação de relação fonte-produto do dossiê MOIS v1. */
public record DossierSourceProductMatchOutput(long dossierId, String status, String businessDecision) {
}
