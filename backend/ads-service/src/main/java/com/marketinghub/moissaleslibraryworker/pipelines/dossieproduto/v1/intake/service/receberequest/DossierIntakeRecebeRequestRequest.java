package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa intake do dossiê MOIS v1. */
public record DossierIntakeRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
