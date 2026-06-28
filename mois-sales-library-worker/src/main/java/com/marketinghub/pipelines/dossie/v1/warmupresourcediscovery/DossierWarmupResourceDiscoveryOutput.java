package com.marketinghub.pipelines.dossie.v1.warmupresourcediscovery;

import java.util.Map;

/** Representa a saída funcional da etapa descoberta de recursos de aquecimento do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryOutput(long dossierId, String status, String businessDecision, Map<String, Object> evidence) {
}
