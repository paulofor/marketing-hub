package com.marketinghub.pipelines.dossie.v1;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Descreve a saída estruturada de uma etapa executada pelo pipeline de dossiê MOIS v1. */
public record StageResult(
        String status,
        Map<String, Object> output,
        List<StageArtifact> artifacts,
        String errorMessage,
        List<OpenAiInteraction> openAiInteractions) {
    /** Cria um resultado concluído com saída funcional e artefatos auditáveis. */
    public static StageResult done(Map<String, Object> output, List<StageArtifact> artifacts) {
        return new StageResult("DONE", output, artifacts, null, List.of());
    }

    /** Cria um resultado concluído com saída funcional, artefatos e auditoria bruta da OpenAI. */
    public static StageResult doneWithOpenAiInteractions(
            Map<String, Object> output,
            List<StageArtifact> artifacts,
            List<OpenAiInteraction> openAiInteractions) {
        return new StageResult("DONE", output, artifacts, null, safeInteractions(openAiInteractions));
    }

    /** Normaliza interações nulas para lista vazia e evita checks defensivos no runner. */
    public StageResult {
        openAiInteractions = safeInteractions(openAiInteractions);
    }

    /** Indica se a etapa executada acessou OpenAI e gerou payloads brutos para auditoria no backend. */
    public boolean hasOpenAiInteractions() {
        return !openAiInteractions.isEmpty();
    }

    /** Normaliza uma lista opcional de interações OpenAI para contrato imutável não nulo. */
    private static List<OpenAiInteraction> safeInteractions(List<OpenAiInteraction> interactions) {
        return interactions == null ? List.of() : List.copyOf(interactions);
    }

    /** Representa exatamente uma chamada feita à OpenAI por uma etapa do dossiê v1. */
    public record OpenAiInteraction(
            String rawRequestSent,
            String rawResponseReceived,
            Integer quantidadeTokenEntrada,
            Integer quantidadeTokenSaida,
            BigDecimal custo,
            String modelo,
            String descricaoErro) {
        /** Cria uma interação de sucesso preservando request e response brutos da OpenAI. */
        public static OpenAiInteraction success(String rawRequestSent, String rawResponseReceived, String modelo) {
            return new OpenAiInteraction(rawRequestSent, rawResponseReceived, null, null, null, modelo, null);
        }
    }
}
