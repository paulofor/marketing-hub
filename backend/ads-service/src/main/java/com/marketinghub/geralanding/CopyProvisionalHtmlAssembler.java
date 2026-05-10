package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class CopyProvisionalHtmlAssembler {

    private final ObjectMapper objectMapper;

    public CopyProvisionalHtmlAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String assemble(String copyModelResponse, String wireframeModelResponse, String jobId) {
        if (!StringUtils.hasText(copyModelResponse) || !StringUtils.hasText(wireframeModelResponse)) {
            return null;
        }
        try {
            Map<String, Object> wireframeRoot = objectMapper.readValue(wireframeModelResponse, Map.class);
            Map<String, Object> wireframe = wireframeRoot.get("landingPageWireframe") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : wireframeRoot;

            WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
            String html = generator.generateFromJson(objectMapper.writeValueAsString(wireframe));

            Map<String, Object> copyRoot = objectMapper.readValue(copyModelResponse, Map.class);
            Map<String, Object> copy = copyRoot.get("landingPageCopy") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : copyRoot;

            String headline = readText(copy, "headline");
            Map<String, Object> hero = copy.get("hero") instanceof Map<?, ?> h ? (Map<String, Object>) h : Map.of();
            String heroHeadline = firstNonBlank(headline, readText(hero, "headline"), readText(hero, "promise"));
            String supporting = firstNonBlank(readText(hero, "supportingCopy"), readText(copy, "summary"), readText(copy, "lead"));

            if (StringUtils.hasText(heroHeadline)) {
                html = html.replaceFirst("(?is)<h1>.*?</h1>", "<h1>" + escapeHtml(heroHeadline) + "</h1>");
                html = html.replaceFirst("(?is)<title>.*?</title>", "<title>" + escapeHtml(heroHeadline) + "</title>");
            }
            if (StringUtils.hasText(supporting)) {
                html = html.replaceFirst("(?is)<p>.*?</p>", "<p>" + escapeHtml(supporting) + "</p>");
            }
            return appendJobIdCommentBeforeHead(html, jobId);
        } catch (Exception e) {
            return null;
        }
    }

    private String readText(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
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

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
