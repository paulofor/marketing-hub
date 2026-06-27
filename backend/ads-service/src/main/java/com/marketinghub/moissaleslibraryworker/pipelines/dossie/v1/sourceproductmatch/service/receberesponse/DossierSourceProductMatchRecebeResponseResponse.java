package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa sourceproductmatch do dossiê MOIS v1. */
public record DossierSourceProductMatchRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
