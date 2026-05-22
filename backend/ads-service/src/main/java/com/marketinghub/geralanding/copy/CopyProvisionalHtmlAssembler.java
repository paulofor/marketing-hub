package com.marketinghub.geralanding.copy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
/**
 * Conjunto exclusivo da etapa LANDING_PAGE_COPY: coordena o payload resolver e o processor
 * da etapa de copy para gerar HTML provisório sem invadir responsabilidades das demais etapas.
 */
public class CopyProvisionalHtmlAssembler {
    private static final Logger log = LoggerFactory.getLogger(CopyProvisionalHtmlAssembler.class);

    private final CopyProvisionalHtmlPayloadResolver payloadResolver;
    private final CopyProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    public CopyProvisionalHtmlAssembler(CopyProvisionalHtmlPayloadResolver payloadResolver,
                                        CopyProvisionalHtmlProcessor processor,
                                        ObjectMapper objectMapper) {
        this.payloadResolver = payloadResolver;
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    public String assemble(String copyModelResponse, String wireframeModelResponse, String jobId) {
        if (!StringUtils.hasText(copyModelResponse) || !StringUtils.hasText(wireframeModelResponse)) {
            return null;
        }
        try {
            CopyProvisionalHtmlPayloadResolver.CopyProvisionalHtmlPayload payload =
                    payloadResolver.resolve(copyModelResponse, wireframeModelResponse);

            return processor.process(payloadResolverToJson(payload.wireframe()), payloadResolverToJson(payload.copy()));
        } catch (Exception e) {
            log.error(
                    "Falha ao montar HTML provisório (jobId={}, copyLength={}, wireframeLength={}, copyPreview={}, wireframePreview={})",
                    jobId,
                    lengthOf(copyModelResponse),
                    lengthOf(wireframeModelResponse),
                    preview(copyModelResponse),
                    preview(wireframeModelResponse),
                    e);
            throw new IllegalArgumentException("Falha ao montar HTML provisório a partir de wireframe + copy", e);
        }
    }

    /**
     * Retorna um preview seguro para logs de payloads longos.
     */
    private String preview(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }
        return payload.substring(0, Math.min(payload.length(), 160)).replaceAll("\\s+", " ");
    }

    /**
     * Retorna o tamanho do texto para ajudar no diagnóstico do payload.
     */
    private int lengthOf(String payload) {
        return payload == null ? 0 : payload.length();
    }

    /**
     * Serializa o payload resolvido para JSON válido antes do processamento.
     */
    private String payloadResolverToJson(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

}
