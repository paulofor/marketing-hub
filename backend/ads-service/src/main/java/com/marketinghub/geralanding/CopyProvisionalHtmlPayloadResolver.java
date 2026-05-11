package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CopyProvisionalHtmlPayloadResolver {

    private final ObjectMapper objectMapper;

    public CopyProvisionalHtmlPayloadResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public CopyProvisionalHtmlPayload resolve(String copyModelResponse, String wireframeModelResponse) throws Exception {
        Map<String, Object> wireframeRoot = objectMapper.readValue(wireframeModelResponse, Map.class);
        Map<String, Object> wireframe = wireframeRoot.get("landingPageWireframe") instanceof Map<?, ?> nested
                ? (Map<String, Object>) nested
                : wireframeRoot;

        Map<String, Object> copyRoot = objectMapper.readValue(copyModelResponse, Map.class);
        Map<String, Object> copy = copyRoot.get("landingPageCopy") instanceof Map<?, ?> nested
                ? (Map<String, Object>) nested
                : copyRoot;

        return new CopyProvisionalHtmlPayload(wireframe, copy);
    }

    public record CopyProvisionalHtmlPayload(Map<String, Object> wireframe, Map<String, Object> copy) {
    }
}
