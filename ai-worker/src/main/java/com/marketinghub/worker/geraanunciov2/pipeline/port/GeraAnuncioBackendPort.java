package com.marketinghub.worker.geraanunciov2.pipeline.port;

/** Responsabilidade: definir a comunicação oficial do GeraAnuncio v2 com o backend. */
public interface GeraAnuncioBackendPort {
    /** Busca trabalho pendente pelo endpoint canônico pending da etapa. */
    void fetchPending();
}
