package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa warmupsignalextraction do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
