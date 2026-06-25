package com.marketinghub.mois.dossie.v1.dossiersynthesis.service.pending;

import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa síntese final do dossiê do dossiê MOIS v1. */
public record DossierDossierSynthesisPendingJob(long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
