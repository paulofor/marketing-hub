package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa intake do dossiê MOIS v1. */
public record DossierIntakeRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
