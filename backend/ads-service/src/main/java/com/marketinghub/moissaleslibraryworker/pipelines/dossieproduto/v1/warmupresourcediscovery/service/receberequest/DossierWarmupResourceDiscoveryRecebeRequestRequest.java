package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa warmupresourcediscovery do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
