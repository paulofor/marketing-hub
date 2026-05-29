package com.marketinghub.worker.openai.core.wireframe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;

import java.util.Map;

public class WireframeResponseValidator implements StageResponseValidator<WireframeOutput> {

    private final ObjectMapper objectMapper;

    public WireframeResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public WireframeOutput validateAndParse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            throw new InvalidModelResponseException("Wireframe model response is blank");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    modelResponse,
                    new TypeReference<Map<String, Object>>() {}
            );
            return new WireframeOutput(payload);
        } catch (Exception error) {
            throw new InvalidModelResponseException("Wireframe model response is not valid JSON", error);
        }
    }
}
