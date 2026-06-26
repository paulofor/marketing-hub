package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.qualificafontes;

/** Representa a entrada mínima da etapa qualificafontes do dossiê do produto. */
public record QualificaFontesInput(long jobId, long salesPageId, String workspaceId) {
}
