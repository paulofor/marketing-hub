package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa productunderstanding do dossiê MOIS v1. */
public record DossierProductUnderstandingRecebeRequestRequest(
        @NotBlank String request,
        String plataforma,
        String prompt,
        String schema,
        String promptTemplateKey,
        String promptTemplateVersion,
        String schemaName) {
}
