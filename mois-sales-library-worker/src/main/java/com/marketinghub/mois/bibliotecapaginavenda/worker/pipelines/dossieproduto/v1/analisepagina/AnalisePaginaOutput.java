package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.analisepagina;

/** Representa a saída funcional estruturada da etapa analisepagina do dossiê do produto. */
public record AnalisePaginaOutput(long salesPageId, String status, String summary) {
}
