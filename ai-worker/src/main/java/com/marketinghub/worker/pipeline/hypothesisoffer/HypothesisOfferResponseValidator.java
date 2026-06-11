package com.marketinghub.worker.pipeline.hypothesisoffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para a etapa Oferta. */
public class HypothesisOfferResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(HypothesisOfferResponseValidator.class);
    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para conversão JSON. */
    public HypothesisOfferResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida campos essenciais da oferta e retorna o objeto normalizado da etapa. */
    public HypothesisOfferOutput validateAndParse(String modelResponse) {
        try {
            HypothesisOfferOutput output = objectMapper.readValue(modelResponse, HypothesisOfferOutput.class);
            requireText(output.offerName(), "offerName");
            requireText(output.coreOffer(), "coreOffer");
            requireText(output.howItWorks(), "howItWorks");
            requireText(output.summary(), "summary");
            requireNonEmpty(output.steps(), "steps");
            return output;
        } catch (Exception ex) {
            log.error("Resposta inválida recebida da OpenAI na etapa Oferta", ex);
            throw new StageWorkerException("Resposta inválida da etapa Oferta", ex);
        }
    }

    /** Garante que um campo textual obrigatório veio preenchido. */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StageWorkerException("Campo obrigatório ausente na etapa Oferta: " + field);
        }
    }

    /** Garante que uma lista obrigatória veio preenchida. */
    private void requireNonEmpty(java.util.List<String> value, String field) {
        if (value == null || value.isEmpty()) {
            throw new StageWorkerException("Lista obrigatória ausente na etapa Oferta: " + field);
        }
    }
}
