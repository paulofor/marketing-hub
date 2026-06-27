package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa investigationanchorbuilder do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
