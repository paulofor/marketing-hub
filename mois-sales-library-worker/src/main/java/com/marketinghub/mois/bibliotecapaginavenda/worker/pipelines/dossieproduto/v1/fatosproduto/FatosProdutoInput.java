package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.fatosproduto;

/** Representa a entrada mínima da etapa fatosproduto do dossiê do produto. */
public record FatosProdutoInput(long jobId, long salesPageId, String workspaceId) {
}
