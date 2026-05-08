package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class WireframeProvisionalHtmlAssembler {

    private final ObjectMapper objectMapper;

    public WireframeProvisionalHtmlAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String assemble(String modelResponse) {
        if (!StringUtils.hasText(modelResponse)) {
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(modelResponse, Map.class);
            Map<String, Object> wireframe = root.get("landingPageWireframe") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : root;
            WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
            return generator.generateFromJson(objectMapper.writeValueAsString(wireframe));
        } catch (Exception e) {
            return null;
        }
    }
}
