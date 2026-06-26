package com.marketinghub.pipelines.mois.dossie.v1.warmupresourcediscovery.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa descoberta de recursos de aquecimento do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
