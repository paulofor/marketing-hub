package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.fatosproduto;

/** Representa a saída funcional estruturada da etapa fatosproduto do dossiê do produto. */
public record FatosProdutoOutput(long salesPageId, String status, String summary) {
}
