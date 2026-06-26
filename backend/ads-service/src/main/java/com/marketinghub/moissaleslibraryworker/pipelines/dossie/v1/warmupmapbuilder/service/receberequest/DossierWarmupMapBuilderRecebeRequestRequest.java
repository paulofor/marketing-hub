package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa warmupmapbuilder do dossiê MOIS v1. */
public record DossierWarmupMapBuilderRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
