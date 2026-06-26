package com.marketinghub.pipelines.dossie.v1;

import java.util.List;
import java.util.Optional;

/** Orquestra processors plugáveis sem conhecer detalhes das etapas concretas do dossiê MOIS v1. */
public class PipelineWorker {

    private final List<StageProcessor> processors;

    /** Recebe a lista de processors disponíveis para o pipeline v1. */
    public PipelineWorker(List<StageProcessor> processors) {
        this.processors = List.copyOf(processors);
    }

    /** Executa a etapa indicada no contexto usando o processor compatível. */
    public StageResult execute(StageContext context) {
        StageProcessor processor = findProcessor(context.stageName())
                .orElseThrow(() -> new IllegalArgumentException("Etapa de dossiê não suportada: " + context.stageName()));
        return processor.process(context);
    }

    /** Localiza o processor de uma etapa pelo nome canônico. */
    private Optional<StageProcessor> findProcessor(String stageName) {
        return processors.stream().filter(processor -> processor.stageName().equals(stageName)).findFirst();
    }
}
