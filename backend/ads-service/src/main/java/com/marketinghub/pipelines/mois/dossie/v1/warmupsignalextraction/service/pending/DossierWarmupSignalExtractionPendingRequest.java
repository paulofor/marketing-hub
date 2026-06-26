package com.marketinghub.pipelines.mois.dossie.v1.warmupsignalextraction.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa extração de sinais de aquecimento do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
