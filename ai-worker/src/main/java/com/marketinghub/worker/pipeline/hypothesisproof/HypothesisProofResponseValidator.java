package com.marketinghub.worker.pipeline.hypothesisproof;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para a etapa Prova. */
public class HypothesisProofResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(HypothesisProofResponseValidator.class);
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para conversão JSON. */
    public HypothesisProofResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida campos essenciais da prova e retorna o objeto normalizado da etapa. */
    public HypothesisProofOutput validateAndParse(String modelResponse) {
        try {
            HypothesisProofOutput output = objectMapper.readValue(modelResponse, HypothesisProofOutput.class);
            requireText(output.proofType(), "proofType");
            requireText(output.proofAsset(), "proofAsset");
            requireText(output.proofMessage(), "proofMessage");
            requireText(output.summary(), "summary");
            requireNonEmpty(output.evidenceSignals(), "evidenceSignals");
            return output;
        } catch (Exception ex) {
            log.error("Resposta inválida recebida da OpenAI na etapa Prova", ex);
            throw new StageWorkerException("Resposta inválida da etapa Prova", ex);
        }
    }

    /** Garante que um campo textual obrigatório veio preenchido. */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StageWorkerException("Campo obrigatório ausente na etapa Prova: " + field);
        }
    }

    /** Garante que uma lista obrigatória veio preenchida. */
    private void requireNonEmpty(java.util.List<String> value, String field) {
        if (value == null || value.isEmpty()) {
            throw new StageWorkerException("Lista obrigatória ausente na etapa Prova: " + field);
        }
    }
}
