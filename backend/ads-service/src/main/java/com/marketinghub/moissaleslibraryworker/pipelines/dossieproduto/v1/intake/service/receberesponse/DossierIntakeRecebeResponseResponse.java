package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa intake do dossiê MOIS v1. */
public record DossierIntakeRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
