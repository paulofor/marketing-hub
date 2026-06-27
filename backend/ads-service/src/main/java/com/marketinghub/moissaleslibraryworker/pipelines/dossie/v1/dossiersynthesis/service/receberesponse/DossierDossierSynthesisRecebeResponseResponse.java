package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa dossiersynthesis do dossiê MOIS v1. */
public record DossierDossierSynthesisRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
