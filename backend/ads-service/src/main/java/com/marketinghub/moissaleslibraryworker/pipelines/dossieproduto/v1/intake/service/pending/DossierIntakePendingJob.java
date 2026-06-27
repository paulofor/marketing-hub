package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.pending;

import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakePendingJob(String jobId, long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
