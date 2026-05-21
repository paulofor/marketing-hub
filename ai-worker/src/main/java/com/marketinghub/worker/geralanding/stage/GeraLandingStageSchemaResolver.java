package com.marketinghub.worker.geralanding.stage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Resolve exclusivamente o schema por etapa no Worker AI, mantendo isolamento por conjunto.
 */
@Component
public class GeraLandingStageSchemaResolver {

    private final ObjectMapper objectMapper;

    public GeraLandingStageSchemaResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> resolveSchema(GeraLandingStageDefinition stage,
                                             Resource wireframeSchemaResource,
                                             Resource copySchemaResource,
                                             Resource imagePlanningSchemaResource,
                                             Resource designPresetSchemaResource,
                                             Resource deliverablesSchemaResource) throws JsonProcessingException {
        Resource schemaResource = switch (stage) {
            case COPY -> copySchemaResource;
            case IMAGE_PLANNING -> imagePlanningSchemaResource;
            case DESIGN_PRESET -> designPresetSchemaResource;
            case DELIVERABLES -> deliverablesSchemaResource;
            case WIREFRAME -> wireframeSchemaResource;
        };
        try {
            return objectMapper.readValue(schemaResource.getInputStream(), Map.class);
        } catch (IOException ex) {
            throw new JsonProcessingException("Falha ao carregar schema da etapa " + stage.code()) {
            };
        }
    }
}
