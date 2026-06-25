package com.marketinghub.mois.dossie.v1.productunderstanding.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa entendimento do produto do dossiê MOIS v1. */
public record DossierProductUnderstandingPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
