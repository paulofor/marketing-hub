package com.marketinghub.pipelines.dossie.v1.intake;

/** Representa a saída funcional da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakeOutput(long dossierId, String status, String businessDecision) {
}
