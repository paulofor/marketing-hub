package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa dossiersynthesis do dossiê MOIS v1. */
public record DossierDossierSynthesisRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
