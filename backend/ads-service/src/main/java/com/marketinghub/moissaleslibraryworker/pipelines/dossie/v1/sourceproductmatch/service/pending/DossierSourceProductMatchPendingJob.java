package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending;

import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa validação de relação fonte-produto do dossiê MOIS v1. */
public record DossierSourceProductMatchPendingJob(long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
