package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                String uiSizeTexts = asText(section.get("uiSizeTexts"));
                if (StringUtils.hasText(uiTags)) {
                    sectionsHtml.append(uiTags.trim()).append("\n");
                }
                if (StringUtils.hasText(uiSizeTexts)) {
                    sectionsHtml.append(buildLoremPreview(uiSizeTexts));
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

    private String buildLoremPreview(String uiSizeTexts) {
        List<TextSlotSpec> specs = parseTextSpecs(uiSizeTexts);
        if (specs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n<!-- lhm-text-preview -->\n");
        for (TextSlotSpec spec : specs) {
            sb.append("<p data-wireframe-lorem-slot=\"")
                    .append(spec.slotId())
                    .append("\">")
                    .append(generateLorem(spec.maxLength() > 0 ? spec.maxLength() : spec.minLength()))
                    .append("</p>\n");
        }
        return sb.toString();
    }

    private List<TextSlotSpec> parseTextSpecs(String uiSizeTexts) {
        if (!StringUtils.hasText(uiSizeTexts)) {
            return List.of();
        }
        List<TextSlotSpec> specs = new ArrayList<>();
        for (String token : uiSizeTexts.split(";")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            String trimmed = token.trim();
            String slotId = trimmed.contains("(") ? trimmed.substring(0, trimmed.indexOf('(')).trim() : trimmed;
            int min = extractBound(trimmed, "min");
            int max = extractBound(trimmed, "max");
            if (!StringUtils.hasText(slotId) || (min <= 0 && max <= 0)) {
                continue;
            }
            specs.add(new TextSlotSpec(slotId, min, max));
        }
        return specs;
    }

    private int extractBound(String spec, String key) {
        Matcher matcher = Pattern.compile(key + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(spec);
        if (!matcher.find()) {
            return -1;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String generateLorem(int length) {
        if (length <= 0) {
            return "";
        }
        String seed = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(seed);
        }
        return sb.substring(0, Math.min(length, sb.length())).trim();
    }

    private record TextSlotSpec(String slotId, int minLength, int maxLength) {}
}
