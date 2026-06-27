package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa dossiersynthesis do dossiê MOIS v1. */
public record DossierDossierSynthesisRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
