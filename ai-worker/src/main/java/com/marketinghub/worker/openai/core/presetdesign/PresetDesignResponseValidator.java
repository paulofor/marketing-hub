package com.marketinghub.worker.openai.core.presetdesign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: validar e converter a resposta JSON da etapa presetdesign. */
public class PresetDesignResponseValidator implements StageResponseValidator<PresetDesignOutput> {

    private static final Logger log = LoggerFactory.getLogger(PresetDesignResponseValidator.class);

    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para ler a resposta da OpenAI. */
    public PresetDesignResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida que a resposta do modelo é JSON e retorna o payload estruturado. */
    @Override
    public PresetDesignOutput validateAndParse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            throw new InvalidModelResponseException("PresetDesign model response is blank");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    modelResponse,
                    new TypeReference<Map<String, Object>>() {}
            );
            return new PresetDesignOutput(payload);
        } catch (Exception error) {
            log.warn(
                    "PresetDesign model response is not valid JSON. responseLength={}",
                    modelResponse.length(),
                    error
            );
            throw new InvalidModelResponseException("PresetDesign model response is not valid JSON", error);
        }
    }
}
