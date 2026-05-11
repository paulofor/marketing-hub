package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
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

            WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
            String html = generator.generateFromJson(payloadResolverToJson(payload));
            html = processor.process(html, payload.copy());
            return appendJobIdCommentBeforeHead(html, jobId);
        } catch (Exception e) {
            return null;
        }
    }

    private String payloadResolverToJson(CopyProvisionalHtmlPayloadResolver.CopyProvisionalHtmlPayload payload) throws Exception {
        return objectMapper.writeValueAsString(payload.wireframe());
    }

    private String appendJobIdCommentBeforeHead(String html, String jobId) {
        if (!StringUtils.hasText(html) || !StringUtils.hasText(jobId)) {
            return html;
        }
        String comment = "<!-- jobId = " + jobId + " -->\n";
        int headIndex = html.toLowerCase().indexOf("<head>");
        if (headIndex < 0) {
            return comment + html;
        }
        return html.substring(0, headIndex) + comment + html.substring(headIndex);
    }

}
