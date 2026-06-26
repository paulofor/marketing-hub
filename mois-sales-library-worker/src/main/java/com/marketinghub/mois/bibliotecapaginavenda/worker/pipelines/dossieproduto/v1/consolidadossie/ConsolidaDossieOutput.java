package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.consolidadossie;

/** Representa a saída funcional estruturada da etapa consolidadossie do dossiê do produto. */
public record ConsolidaDossieOutput(long salesPageId, String status, String summary) {
}
