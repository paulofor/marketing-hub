package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1;

import java.util.Map;

/** Carrega o contexto auditável comum recebido do backend para uma etapa do dossiê do produto. */
public record StageContext(long jobId, long salesPageId, String workspaceId, Map<String, Object> input) {
}
