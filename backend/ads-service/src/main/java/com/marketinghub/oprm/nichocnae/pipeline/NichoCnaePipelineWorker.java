package com.marketinghub.oprm.nichocnae.pipeline;

/** Responsabilidade: definir a porta genérica de orquestração das etapas do pipeline OPRM NichoCNAE. */
public interface NichoCnaePipelineWorker {
    /** Executa a próxima etapa aplicável usando apenas contratos genéricos do núcleo do pipeline. */
    NichoCnaeStageResult runNext(NichoCnaeStageContext context);
}
