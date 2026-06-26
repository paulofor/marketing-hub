package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa sourceproductmatch do dossiê MOIS v1. */
public record DossierSourceProductMatchRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
