package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa sourceproductmatch do dossiê MOIS v1. */
public record DossierSourceProductMatchRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
