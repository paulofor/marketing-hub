package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.receberequest;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada do endpoint recebeRequest da etapa warmupsignalextraction do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionRecebeRequestRequest(@NotBlank String request, String plataforma, String prompt, String schema) {
}
