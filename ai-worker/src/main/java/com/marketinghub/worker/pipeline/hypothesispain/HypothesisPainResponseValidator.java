package com.marketinghub.worker.pipeline.hypothesispain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para a etapa Dor. */
public class HypothesisPainResponseValidator {
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para conversão JSON. */
    public HypothesisPainResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida campos essenciais da dor e retorna o objeto normalizado da etapa. */
    public HypothesisPainOutput validateAndParse(String modelResponse) {
        try {
            HypothesisPainOutput output = objectMapper.readValue(modelResponse, HypothesisPainOutput.class);
            requireText(output.surface(), "surface");
            requireText(output.root(), "root");
            requireText(output.summary(), "summary");
            return output;
        } catch (Exception ex) {
            throw new StageWorkerException("Resposta inválida da etapa Dor", ex);
        }
    }

    /** Garante que um campo textual obrigatório veio preenchido. */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StageWorkerException("Campo obrigatório ausente na etapa Dor: " + field);
        }
    }
}
