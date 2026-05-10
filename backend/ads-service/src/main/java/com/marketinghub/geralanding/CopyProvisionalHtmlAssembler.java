package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

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

            Deque<String> copyTexts = collectCopyTexts(copy);
            html = applyCopyToAllTextNodes(html, copyTexts);
            if (StringUtils.hasText(copyTexts.peekFirst())) {
                html = html.replaceFirst("(?is)<title>.*?</title>", "<title>" + escapeHtml(copyTexts.peekFirst()) + "</title>");
            }
            return appendJobIdCommentBeforeHead(html, jobId);
        } catch (Exception e) {
            return null;
        }
    }

    private String applyCopyToAllTextNodes(String html, Deque<String> copyTexts) {
        if (!StringUtils.hasText(html)) {
            throw new IllegalArgumentException("HTML provisório ausente para aplicar a copy");
        }
        if (copyTexts.isEmpty()) {
            throw new IllegalArgumentException("Copy da etapa Gera Copy sem textos para preencher a página");
        }

        String[] tags = {"h1", "h2", "h3", "p", "li", "span", "summary", "a", "button"};
        String result = html;
        for (String tag : tags) {
            String pattern = "(?is)<" + tag + "(\\s[^>]*)?>.*?</" + tag + ">";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(result);
            StringBuffer rewritten = new StringBuffer();
            while (matcher.find()) {
                String text = copyTexts.pollFirst();
                if (!StringUtils.hasText(text)) {
                    throw new IllegalArgumentException("Copy insuficiente para preencher todos os campos de texto do HTML");
                }
                String attrs = matcher.group(1) == null ? "" : matcher.group(1);
                String replacement = "<" + tag + attrs + ">" + escapeHtml(text) + "</" + tag + ">";
                matcher.appendReplacement(rewritten, java.util.regex.Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(rewritten);
            result = rewritten.toString();
        }
        return result;
    }

    private Deque<String> collectCopyTexts(Map<String, Object> copy) {
        List<String> values = new ArrayList<>();
        collectTextsRecursive(copy, null, values);
        Deque<String> queue = new ArrayDeque<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                queue.addLast(value.trim());
            }
        }
        return queue;
    }

    @SuppressWarnings("unchecked")
    private void collectTextsRecursive(Object node, String key, List<String> out) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                collectTextsRecursive(entry.getValue(), String.valueOf(entry.getKey()), out);
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectTextsRecursive(item, key, out);
            }
            return;
        }
        if (node instanceof String text && shouldUseAsCopyText(key, text)) {
            out.add(text);
        }
    }
    private boolean shouldUseAsCopyText(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (key == null) {
            return true;
        }
        String normalized = key.toLowerCase();
        if (normalized.contains("id")
                || normalized.contains("slot")
                || normalized.contains("section")
                || normalized.contains("url")
                || normalized.contains("placement")
                || normalized.contains("code")
                || normalized.contains("status")) {
            return false;
        }
        return true;
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
