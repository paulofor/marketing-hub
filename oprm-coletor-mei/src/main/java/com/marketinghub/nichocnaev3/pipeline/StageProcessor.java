package com.marketinghub.nichocnaev3.pipeline;

/** Contrato que toda etapa concreta plugável do NichoCNAE v3 deve implementar. */
public interface StageProcessor {
    /** Executa a etapa com o contexto persistido recebido do backend. */
    StageResult process(StageContext context);
}
