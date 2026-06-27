package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberesponse;

/** Contrato de saída do endpoint recebeResponse da etapa warmupresourcediscovery do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryRecebeResponseResponse(
        String jobId, String productKey, String stageCode, String status, String nextStageCode) {
}
