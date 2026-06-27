package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberequest;

/** Contrato de resposta do endpoint recebeRequest da etapa warmupresourcediscovery do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryRecebeRequestResponse(String jobId, String idExterno, String codigoEtapa, String status) {
}
