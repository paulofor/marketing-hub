package com.marketinghub.geralanding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CopyProvisionalHtmlProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String process(String wireframeJson, String copyJson) {
        if (!StringUtils.hasText(wireframeJson)) {
            throw new IllegalArgumentException("JSON de wireframe ausente");
        }
        if (!StringUtils.hasText(copyJson)) {
            throw new IllegalArgumentException("JSON de copy ausente");
        }

        Map<String, Object> wireframe = parseJson(wireframeJson);
        Map<String, Object> copy = parseJson(copyJson);

        String baseHtml = buildHtmlFromWireframe(wireframe);
        Map<String, String> copyByItemId = collectCopyByItemId(copy);

        Document document = Jsoup.parse(baseHtml, "", Parser.htmlParser());
        for (Map.Entry<String, String> entry : copyByItemId.entrySet()) {
            Element element = document.getElementById(entry.getKey());
            if (element != null && StringUtils.hasText(entry.getValue())) {
                element.text(entry.getValue().trim());
            }
        }

        String title = firstNonBlank(copyByItemId.get("s1-title"), copyByItemId.get("s2-title"));
        if (StringUtils.hasText(title)) {
            document.title(title);
        }

        return document.outerHtml();
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar JSON de entrada", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String buildHtmlFromWireframe(Map<String, Object> wireframe) {
        Object rawSections = wireframe.get("sectionOrder");
        if (!(rawSections instanceof List<?> sections) || sections.isEmpty()) {
            throw new IllegalArgumentException("Wireframe sem sectionOrder para montar HTML base");
        }

        List<String> sectionBlocks = new ArrayList<>();
        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)) {
                continue;
            }
            String uiTags = asString(sectionMap.get("uiTags"));
            if (StringUtils.hasText(uiTags)) {
                sectionBlocks.add(uiTags.trim());
            }
        }

        if (sectionBlocks.isEmpty()) {
            throw new IllegalArgumentException("Wireframe sem uiTags para montar HTML base");
        }

        return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Landing Provisória</title></head><body>"
                + String.join("\n", sectionBlocks)
                + "</body></html>";
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> collectCopyByItemId(Map<String, Object> copyJson) {
        Map<String, String> result = new LinkedHashMap<>();
        Object rawSections = copyJson.get("bodySections");
        if (!(rawSections instanceof List<?> sections)) {
            return result;
        }

        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)) {
                continue;
            }
            Object rawItems = sectionMap.get("items");
            if (!(rawItems instanceof List<?> items)) {
                continue;
            }
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> itemMap)) {
                    continue;
                }
                String id = asString(itemMap.get("item"));
                String copy = asString(itemMap.get("copy"));
                if (StringUtils.hasText(id) && StringUtils.hasText(copy)) {
                    result.put(id.trim(), copy.trim());
                }
            }
        }
        return result;
    }

    private String asString(Object value) {
        return value instanceof String str ? str : null;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return StringUtils.hasText(second) ? second : null;
    }
}
