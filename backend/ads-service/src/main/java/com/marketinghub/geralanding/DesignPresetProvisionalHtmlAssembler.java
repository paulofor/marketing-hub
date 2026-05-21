package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DesignPresetProvisionalHtmlAssembler {

    private final DesignPresetProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    public DesignPresetProvisionalHtmlAssembler(DesignPresetProvisionalHtmlProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String assemble(String wireframeJson,
                           String copyJson,
                           String imagePlanningJson,
                           String designPresetOutputJson,
                           String jobId) {
        if (!StringUtils.hasText(wireframeJson)
                || !StringUtils.hasText(copyJson)
                || !StringUtils.hasText(designPresetOutputJson)) {
            return null;
        }

        try {
            Map<String, Object> wireframePayload = normalizePayload(wireframeJson, "landingPageWireframe");
            Map<String, Object> copyPayload = normalizePayload(copyJson, "landingPageCopy");
            String html = processor.process(
                    objectMapper.writeValueAsString(wireframePayload),
                    objectMapper.writeValueAsString(copyPayload),
                    imagePlanningJson,
                    designPresetOutputJson);
            return appendJobIdCommentBeforeHead(html, jobId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao montar HTML provisório da fase landing-page-design-preset", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizePayload(String sourceJson, String preferredRoot) throws Exception {
        Map<String, Object> root = objectMapper.readValue(sourceJson, Map.class);
        if (root.get(preferredRoot) instanceof Map<?, ?> nested) {
            return (Map<String, Object>) nested;
        }
        return new LinkedHashMap<>(root);
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
