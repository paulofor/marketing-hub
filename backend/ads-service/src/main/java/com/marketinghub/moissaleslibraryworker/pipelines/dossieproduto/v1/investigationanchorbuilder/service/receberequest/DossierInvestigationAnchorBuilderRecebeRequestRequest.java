package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa investigationanchorbuilder do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
