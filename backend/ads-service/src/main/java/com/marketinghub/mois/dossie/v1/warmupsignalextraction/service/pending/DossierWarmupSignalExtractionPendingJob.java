package com.marketinghub.mois.dossie.v1.warmupsignalextraction.service.pending;

import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa extração de sinais de aquecimento do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionPendingJob(long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
