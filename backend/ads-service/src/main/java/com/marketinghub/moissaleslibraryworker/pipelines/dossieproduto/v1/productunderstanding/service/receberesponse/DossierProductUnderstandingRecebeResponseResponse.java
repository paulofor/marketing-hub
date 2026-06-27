package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa productunderstanding do dossiê MOIS v1. */
public record DossierProductUnderstandingRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
