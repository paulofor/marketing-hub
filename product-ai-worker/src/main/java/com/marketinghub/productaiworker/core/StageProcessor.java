package com.marketinghub.productaiworker.core;

/** Responsabilidade: definir o contrato de processamento de uma etapa do Product AI Worker. */
public interface StageProcessor {
    /** Retorna o código do pipeline atendido pelo processor. */
    String pipelineCode();

    /** Retorna o código da etapa atendida pelo processor. */
    String stageCode();

    /** Processa uma execução pendente e reporta o resultado ao backend. */
    void process(StageContext context);
}
