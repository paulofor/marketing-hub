package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1;

/** Representa um artefato auditável produzido por uma etapa do dossiê do produto. */
public record StageArtifact(String type, String storageKey, String payload) {
}
