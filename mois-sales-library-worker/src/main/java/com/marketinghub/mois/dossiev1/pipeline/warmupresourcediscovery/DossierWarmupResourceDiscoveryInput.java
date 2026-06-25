package com.marketinghub.mois.dossiev1.pipeline.warmupresourcediscovery;

import java.util.Map;

/** Representa a entrada funcional da etapa descoberta de recursos de aquecimento do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
