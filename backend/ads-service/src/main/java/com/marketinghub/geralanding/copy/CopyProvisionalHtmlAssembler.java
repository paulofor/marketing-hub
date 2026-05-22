package com.marketinghub.geralanding.copy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
/**
 * Conjunto exclusivo da etapa LANDING_PAGE_COPY: coordena o payload resolver e o processor
 * da etapa de copy para gerar HTML provisório sem invadir responsabilidades das demais etapas.
 */
public class CopyProvisionalHtmlAssembler {

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
            throw new IllegalArgumentException("Falha ao montar HTML provisório a partir de wireframe + copy", e);
        }
    }
    private String payloadResolverToJson(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

}
