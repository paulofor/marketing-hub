package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared;

import com.marketinghub.moissaleslibraryworker.pipelines.shared.service.OpenAiTextResponseExtractor;

/** Utilitário responsável por extrair a resposta funcional limpa dos envelopes OpenAI do dossiê de produto. */
public final class DossierOpenAiTextResponseExtractor {
    /** Impede instanciação porque o extrator não mantém estado por execução. */
    private DossierOpenAiTextResponseExtractor() {}

    /** Delega a extração para o utilitário compartilhado entre pipelines MOIS. */
    public static String extract(String rawResponse) {
        return OpenAiTextResponseExtractor.extract(rawResponse);
    }
}
