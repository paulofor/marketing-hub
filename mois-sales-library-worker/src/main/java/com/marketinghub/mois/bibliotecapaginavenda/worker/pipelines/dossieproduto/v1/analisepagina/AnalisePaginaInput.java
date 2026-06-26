package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.analisepagina;

/** Representa a entrada mínima da etapa analisepagina do dossiê do produto. */
public record AnalisePaginaInput(long jobId, long salesPageId, String workspaceId) {
}
