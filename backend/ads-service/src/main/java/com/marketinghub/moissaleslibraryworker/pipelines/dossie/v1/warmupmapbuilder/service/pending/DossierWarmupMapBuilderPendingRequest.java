package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa montagem do mapa de aquecimento do dossiê MOIS v1. */
public record DossierWarmupMapBuilderPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
