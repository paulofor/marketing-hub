package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa geração de âncoras de investigação do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
