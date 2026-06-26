package com.marketinghub.pipelines.mois.dossie.v1.sourceproductmatch.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa validação de relação fonte-produto do dossiê MOIS v1. */
public record DossierSourceProductMatchPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
