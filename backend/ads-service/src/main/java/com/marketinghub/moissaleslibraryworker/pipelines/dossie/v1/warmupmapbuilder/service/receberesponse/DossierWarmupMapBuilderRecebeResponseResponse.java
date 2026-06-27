package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa warmupmapbuilder do dossiê MOIS v1. */
public record DossierWarmupMapBuilderRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
