package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa productunderstanding do dossiê MOIS v1. */
public record DossierProductUnderstandingRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
