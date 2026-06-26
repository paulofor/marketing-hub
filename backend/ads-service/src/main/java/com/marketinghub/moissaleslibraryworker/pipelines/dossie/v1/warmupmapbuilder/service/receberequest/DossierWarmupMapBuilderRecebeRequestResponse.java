package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa warmupmapbuilder do dossiê MOIS v1. */
public record DossierWarmupMapBuilderRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
