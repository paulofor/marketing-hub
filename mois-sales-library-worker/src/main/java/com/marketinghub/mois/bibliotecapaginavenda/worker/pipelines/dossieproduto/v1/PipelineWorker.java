package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1;

import java.util.List;
import java.util.Optional;

/** Orquestra localmente processors plugáveis sem conhecer a implementação concreta das etapas. */
public class PipelineWorker {

    private final List<StageProcessor> processors;

    /** Recebe o catálogo de processors disponíveis para execução pelo módulo. */
    public PipelineWorker(List<StageProcessor> processors) {
        this.processors = List.copyOf(processors);
    }

    /** Executa uma etapa pelo nome canônico quando houver processor registrado. */
    public Optional<StageResult> process(String stageName, StageContext context) {
        return processors.stream()
                .filter(processor -> processor.stageName().equals(stageName))
                .findFirst()
                .map(processor -> processor.process(context));
    }
}
