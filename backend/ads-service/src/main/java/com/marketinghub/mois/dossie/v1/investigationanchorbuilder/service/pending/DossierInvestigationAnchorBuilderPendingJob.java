package com.marketinghub.mois.dossie.v1.investigationanchorbuilder.service.pending;

import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa geração de âncoras de investigação do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderPendingJob(long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
