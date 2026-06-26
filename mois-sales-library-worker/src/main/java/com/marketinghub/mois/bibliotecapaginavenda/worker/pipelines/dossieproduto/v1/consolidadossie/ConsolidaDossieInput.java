package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.consolidadossie;

/** Representa a entrada mínima da etapa consolidadossie do dossiê do produto. */
public record ConsolidaDossieInput(long jobId, long salesPageId, String workspaceId) {
}
