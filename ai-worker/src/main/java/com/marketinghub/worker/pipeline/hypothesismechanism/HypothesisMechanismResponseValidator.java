package com.marketinghub.worker.pipeline.hypothesismechanism;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para a etapa Mecanismo. */
public class HypothesisMechanismResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(HypothesisMechanismResponseValidator.class);
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para conversão JSON. */
    public HypothesisMechanismResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida campos essenciais do mecanismo e retorna o objeto normalizado da etapa. */
    public HypothesisMechanismOutput validateAndParse(String modelResponse) {
        try {
            HypothesisMechanismOutput output = objectMapper.readValue(modelResponse, HypothesisMechanismOutput.class);
            requireText(output.mechanismName(), "mechanismName");
            requireText(output.coreMechanism(), "coreMechanism");
            requireText(output.howItWorks(), "howItWorks");
            requireText(output.summary(), "summary");
            requireNonEmpty(output.steps(), "steps");
            return output;
        } catch (Exception ex) {
            log.error("Resposta inválida recebida da OpenAI na etapa Mecanismo", ex);
            throw new StageWorkerException("Resposta inválida da etapa Mecanismo", ex);
        }
    }

    /** Garante que um campo textual obrigatório veio preenchido. */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StageWorkerException("Campo obrigatório ausente na etapa Mecanismo: " + field);
        }
    }

    /** Garante que uma lista obrigatória veio preenchida. */
    private void requireNonEmpty(java.util.List<String> value, String field) {
        if (value == null || value.isEmpty()) {
            throw new StageWorkerException("Lista obrigatória ausente na etapa Mecanismo: " + field);
        }
    }
}
