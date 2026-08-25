package com.marketinghub.videomanagement.referenceanalysisv1.pipeline;

/** Contrato genérico de uma etapa plugável da análise versionada de vídeos de referência. */
public interface ReferenceAnalysisStageProcessor {
    /** Executa a etapa concreta usando somente o contexto recebido do backend. */
    ReferenceAnalysisStageResult process(ReferenceAnalysisStageContext context);
}
