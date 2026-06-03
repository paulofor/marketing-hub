package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

import org.springframework.stereotype.Component;

/** Mantém artefatos em memória somente até o envio do resultado final ao backend. */
@Component
public class InMemoryArtifactStore implements ArtifactStore {

    /** Gera uma chave lógica rastreável para o artefato textual produzido pela etapa. */
    @Override
    public String putText(long executionId, String artifactType, String contentType, String content) {
        return "memory://pipeline/" + executionId + "/" + artifactType;
    }
}
