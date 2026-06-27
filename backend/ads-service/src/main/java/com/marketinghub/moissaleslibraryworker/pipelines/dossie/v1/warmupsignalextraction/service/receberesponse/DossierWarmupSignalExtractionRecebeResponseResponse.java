package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa warmupsignalextraction do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
