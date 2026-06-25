package com.marketinghub.mois.dossiev1.pipeline.warmupsignalextraction;

/** Representa a saída funcional da etapa extração de sinais de aquecimento do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionOutput(long dossierId, String status, String businessDecision) {
}
