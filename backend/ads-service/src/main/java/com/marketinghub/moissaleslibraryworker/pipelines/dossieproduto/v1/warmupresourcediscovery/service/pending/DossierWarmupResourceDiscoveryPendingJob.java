package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.pending;

import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa descoberta de recursos de aquecimento do dossiê MOIS v1. */
public record DossierWarmupResourceDiscoveryPendingJob(String jobId, long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
