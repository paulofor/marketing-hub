package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
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
            if (!(wireframe.get("sectionOrder") instanceof List<?> rawSections) || rawSections.isEmpty()) {
                return null;
            }
            StringBuilder sectionsHtml = new StringBuilder();
            StringBuilder css = new StringBuilder();
            for (Object rawSection : rawSections) {
                if (!(rawSection instanceof Map<?, ?> rawSectionMap)) {
                    continue;
                }
                Map<String, Object> section = (Map<String, Object>) rawSectionMap;
                String uiTags = asText(section.get("uiTags"));
                String uiSizes = asText(section.get("uiSizes"));
                if (StringUtils.hasText(uiTags)) {
                    sectionsHtml.append(uiTags.trim()).append("\n");
                }
                if (StringUtils.hasText(uiSizes)) {
                    css.append("/* ").append(asText(section.get("sectionId"), "section")).append(" */\n")
                            .append(uiSizes.trim()).append("\n\n");
                }
            }
            if (!StringUtils.hasText(sectionsHtml.toString())) {
                return null;
            }
            return """
                    <!doctype html>
                    <html lang="pt-BR">
                      <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width,initial-scale=1" />
                        <title>Wireframe provisório</title>
                        <style>
                    """ + css + """
                        </style>
                      </head>
                      <body>
                    """ + sectionsHtml + """
                      </body>
                    </html>
                    """;
        } catch (Exception e) {
            return null;
        }
    }

    private String asText(Object value) {
        return asText(value, null);
    }

    private String asText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : fallback;
    }
}
