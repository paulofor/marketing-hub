package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.qualificafontes;

/** Representa a saída funcional estruturada da etapa qualificafontes do dossiê do produto. */
public record QualificaFontesOutput(long salesPageId, String status, String summary) {
}
