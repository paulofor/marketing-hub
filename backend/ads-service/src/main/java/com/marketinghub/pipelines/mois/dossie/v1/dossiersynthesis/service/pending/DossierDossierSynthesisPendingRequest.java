package com.marketinghub.pipelines.mois.dossie.v1.dossiersynthesis.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Contrato de solicitação do endpoint pending da etapa síntese final do dossiê do dossiê MOIS v1. */
public record DossierDossierSynthesisPendingRequest(@NotBlank String workspaceId, @NotBlank String workerId, Integer limit) {
}
