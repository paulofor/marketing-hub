package com.marketinghub.oprm.nichocnae.pipeline;

/** Responsabilidade: definir o contrato genérico para processadores plugáveis de etapas OPRM NichoCNAE. */
public interface NichoCnaeStageProcessor {
    /** Retorna a etapa canônica atendida por este processador. */
    OprmNichoCnaePipelineSection section();

    /** Executa a etapa com contexto genérico e devolve resultado estruturado auditável. */
    NichoCnaeStageResult process(NichoCnaeStageContext context);
}
