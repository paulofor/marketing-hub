package com.marketinghub.pipelines.dossie.v1;

import java.util.Map;

/** Carrega o contexto funcional persistido pelo backend para execução de uma etapa do dossiê MOIS v1. */
public record StageContext(long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
}
