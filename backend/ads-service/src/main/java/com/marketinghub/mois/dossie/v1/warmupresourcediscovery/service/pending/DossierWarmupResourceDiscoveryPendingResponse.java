package com.marketinghub.mois.dossie.v1.warmupresourcediscovery.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa descoberta de recursos de aquecimento do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryPendingResponse(boolean claimed, List<DossierWarmupResourceDiscoveryPendingJob> jobs) {
}
