package com.marketinghub.worker.openai.core.imageplanning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI na etapa image planning. */
public class ImagePlanningResponseValidator implements StageResponseValidator<ImagePlanningOutput> {

    private static final Logger log = LoggerFactory.getLogger(ImagePlanningResponseValidator.class);

    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o ObjectMapper usado para parsear JSON estruturado. */
    public ImagePlanningResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida que a resposta do modelo é JSON e devolve o payload estruturado da etapa. */
    @Override
    public ImagePlanningOutput validateAndParse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            throw new InvalidModelResponseException("ImagePlanning model response is blank");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    modelResponse,
                    new TypeReference<Map<String, Object>>() {}
            );
            return new ImagePlanningOutput(payload);
        } catch (Exception error) {
            log.error("Image planning model response is not valid JSON. responseLength={}", modelResponse.length(), error);
            throw new InvalidModelResponseException("ImagePlanning model response is not valid JSON", error);
        }
    }
}
