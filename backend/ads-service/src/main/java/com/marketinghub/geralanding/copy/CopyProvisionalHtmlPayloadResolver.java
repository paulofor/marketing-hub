package com.marketinghub.geralanding.copy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
/**
 * Conjunto exclusivo da etapa LANDING_PAGE_COPY: resolve e normaliza payloads de wireframe/copy
 * para que o assembler processe um contrato previsível.
 */
public class CopyProvisionalHtmlPayloadResolver {

    private static final Logger log = LoggerFactory.getLogger(CopyProvisionalHtmlPayloadResolver.class);
    private final ObjectMapper objectMapper;

    public CopyProvisionalHtmlPayloadResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public CopyProvisionalHtmlPayload resolve(String copyModelResponse, String wireframeModelResponse) throws Exception {
        log.info(
                "Resolvendo payload da etapa copy (copyLength={}, wireframeLength={})",
                lengthOf(copyModelResponse),
                lengthOf(wireframeModelResponse));
        Map<String, Object> wireframeRoot = objectMapper.readValue(wireframeModelResponse, Map.class);
        Map<String, Object> wireframe = wireframeRoot.get("landingPageWireframe") instanceof Map<?, ?> nested
                ? (Map<String, Object>) nested
                : wireframeRoot;

        Map<String, Object> copyRoot = objectMapper.readValue(copyModelResponse, Map.class);
        Map<String, Object> copy = copyRoot.get("landingPageCopy") instanceof Map<?, ?> nested
                ? (Map<String, Object>) nested
                : copyRoot;

        log.info(
                "Payload resolvido na etapa copy (wireframeRootKeys={}, wireframeKeys={}, copyRootKeys={}, copyKeys={})",
                wireframeRoot.size(),
                wireframe.size(),
                copyRoot.size(),
                copy.size());

        return new CopyProvisionalHtmlPayload(wireframe, copy);
    }

    /**
     * Retorna o tamanho do texto recebido para facilitar rastreabilidade em logs.
     */
    private int lengthOf(String payload) {
        return StringUtils.hasText(payload) ? payload.length() : 0;
    }

    public record CopyProvisionalHtmlPayload(Map<String, Object> wireframe, Map<String, Object> copy) {
    }
}
