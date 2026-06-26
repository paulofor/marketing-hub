package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.planejabuscas;

/** Representa a saída funcional estruturada da etapa planejabuscas do dossiê do produto. */
public record PlanejaBuscasOutput(long salesPageId, String status, String summary) {
}
