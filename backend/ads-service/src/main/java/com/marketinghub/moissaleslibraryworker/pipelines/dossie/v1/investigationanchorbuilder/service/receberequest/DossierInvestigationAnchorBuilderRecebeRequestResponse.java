package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa investigationanchorbuilder do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
