package com.marketinghub.worker.pipeline.hypothesisresult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para a etapa Resultado. */
public class HypothesisResultResponseValidator {
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para conversão JSON. */
    public HypothesisResultResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida campos essenciais do resultado e retorna o objeto normalizado da etapa. */
    public HypothesisResultOutput validateAndParse(String modelResponse) {
        try {
            HypothesisResultOutput output = objectMapper.readValue(modelResponse, HypothesisResultOutput.class);
            requireText(output.desiredOutcome(), "desiredOutcome");
            requireText(output.measurableChange(), "measurableChange");
            requireText(output.summary(), "summary");
            return output;
        } catch (Exception ex) {
            throw new StageWorkerException("Resposta inválida da etapa Resultado", ex);
        }
    }

    /** Garante que um campo textual obrigatório veio preenchido. */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StageWorkerException("Campo obrigatório ausente na etapa Resultado: " + field);
        }
    }
}
