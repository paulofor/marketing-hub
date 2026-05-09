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
    public String assemble(String modelResponse, String jobId) {
        if (!StringUtils.hasText(modelResponse)) {
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(modelResponse, Map.class);
            Map<String, Object> wireframe = root.get("landingPageWireframe") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : root;
            WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
            String html = generator.generateFromJson(objectMapper.writeValueAsString(wireframe));
            return appendJobIdCommentBeforeHead(html, jobId);
        } catch (Exception e) {
            return null;
        }
    }

    public String assemble(String modelResponse) {
        return assemble(modelResponse, null);
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