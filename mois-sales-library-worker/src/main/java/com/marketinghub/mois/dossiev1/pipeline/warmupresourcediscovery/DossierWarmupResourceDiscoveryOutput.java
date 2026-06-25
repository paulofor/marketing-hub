package com.marketinghub.mois.dossiev1.pipeline.warmupresourcediscovery;

/** Representa a saída funcional da etapa descoberta de recursos de aquecimento do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryOutput(long dossierId, String status, String businessDecision) {
}
