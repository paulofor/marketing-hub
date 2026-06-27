package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakePendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
