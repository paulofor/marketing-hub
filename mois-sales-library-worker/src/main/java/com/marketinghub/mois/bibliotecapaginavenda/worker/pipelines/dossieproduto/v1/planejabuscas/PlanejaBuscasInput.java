package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.planejabuscas;

/** Representa a entrada mínima da etapa planejabuscas do dossiê do produto. */
public record PlanejaBuscasInput(long jobId, long salesPageId, String workspaceId) {
}
