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
    private final WireframeHtmlGenerator wireframeHtmlGenerator = new WireframeHtmlGenerator();

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

        return applyCopyByItemId(baseHtml, copyByItemId);
    }

    public String process(String html, Map<String, Object> copyJson) {
        if (!StringUtils.hasText(html)) {
            throw new IllegalArgumentException("HTML provisório ausente para aplicar a copy");
        }
        if (copyJson == null || copyJson.isEmpty()) {
            throw new IllegalArgumentException("Copy da etapa Gera Copy ausente");
        }
        return applyCopyByItemId(html, collectCopyByItemId(copyJson));
    }

    private String applyCopyByItemId(String baseHtml, Map<String, String> copyByItemId) {
        Document document = Jsoup.parse(baseHtml, "", Parser.htmlParser());
        document.outputSettings()
                .prettyPrint(false)
                .charset("utf-8")
                .syntax(Document.OutputSettings.Syntax.html);
        for (Map.Entry<String, String> entry : copyByItemId.entrySet()) {
            Element element = document.getElementById(entry.getKey());
            if (element != null && StringUtils.hasText(entry.getValue())) {
                element.text(entry.getValue().trim());
            }
        }

        String title = firstNonBlank(copyByItemId.get("title"), copyByItemId.get("s1-title"), copyByItemId.get("s2-title"));
        if (StringUtils.hasText(title)) {
            document.title(title);
        }

        return normalizeSerializedHtml(document.outerHtml());
    }

    private String normalizeSerializedHtml(String html) {
        return html
                .replace("/*<![CDATA[*/", "")
                .replace("/*]]>*/", "")
                .replace(" />", "/>");
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
            return buildHtmlFromWireframePaginaModel(wireframe);
        }

        List<String> sectionBlocks = new ArrayList<>();
        List<String> cssBlocks = new ArrayList<>();
        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)) {
                continue;
            }
            String uiTags = asString(sectionMap.get("uiTags"));
            if (StringUtils.hasText(uiTags)) {
                sectionBlocks.add(uiTags.trim());
            }
            String uiSizes = asString(sectionMap.get("uiSizes"));
            if (StringUtils.hasText(uiSizes)) {
                cssBlocks.add(uiSizes.trim());
            }
        }

        if (sectionBlocks.isEmpty()) {
            throw new IllegalArgumentException("Wireframe sem uiTags para montar HTML base");
        }

        return "<!DOCTYPE html>\n\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"utf-8\"/>\n<meta content=\"width=device-width, initial-scale=1\" name=\"viewport\"/>\n<title>Landing Provisória</title>\n<style>\n"
                + "* { box-sizing: border-box; }\n"
                + "html { scroll-behavior: smooth; }\n"
                + "body { font-family: system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif; color: #111827; background: #ffffff; line-height: 1.5; }\n"
                + "img { display: block; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 12px; object-fit: cover; }\n"
                + "button, a[id$=\"-cta\"] { cursor: pointer; }\n"
                + "label { font-weight: 600; }\n"
                + "input, select, textarea, button { font: inherit; }\n"
                + "button { border: 0; border-radius: 10px; padding: 0 16px; font-weight: 700; }\n"
                + "a { color: inherit; }\n\n"
                + String.join("\n", cssBlocks)
                + "\n  </style>\n</head>\n<body" + buildBodyAttributes(wireframe) + ">"
                + String.join("\n", sectionBlocks)
                + "\n</body>\n</html>";
    }

    private String buildHtmlFromWireframePaginaModel(Map<String, Object> wireframe) {
        if (!wireframe.containsKey("pagina")) {
            throw new IllegalArgumentException("Wireframe sem sectionOrder para montar HTML base");
        }
        try {
            String wireframeJson = OBJECT_MAPPER.writeValueAsString(wireframe);
            return wireframeHtmlGenerator.generateFromJson(wireframeJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao montar HTML base para o novo formato de wireframe", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String buildBodyAttributes(Map<String, Object> wireframe) {
        Object rawBodyAttrs = wireframe.get("bodyAttributes");
        if (!(rawBodyAttrs instanceof List<?> attributes)) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Object attribute : attributes) {
            if (!(attribute instanceof Map<?, ?> attrMap)) {
                continue;
            }
            String name = asString(attrMap.get("attribute"));
            String value = asString(attrMap.get("value"));
            if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
                out.append(' ').append(name.trim()).append("=\"").append(value.trim()).append("\"");
            }
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> collectCopyByItemId(Map<String, Object> copyJson) {
        Map<String, String> result = new LinkedHashMap<>();
        Object rawSections = firstNonNull(copyJson.get("bodySections"), copyJson.get("sections"));
        if (!(rawSections instanceof List<?> sections)) {
            return result;
        }

        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)) {
                continue;
            }
            Object rawItems = firstNonNull(sectionMap.get("items"), sectionMap.get("values"), sectionMap.get("fields"));
            if (!(rawItems instanceof List<?> items)) {
                continue;
            }
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> itemMap)) {
                    continue;
                }
                String id = firstNonBlank(
                        asString(itemMap.get("item")),
                        asString(itemMap.get("id")),
                        asString(itemMap.get("tagId"))
                );
                String copy = firstNonBlank(
                        asString(itemMap.get("copy")),
                        asString(itemMap.get("value")),
                        asString(itemMap.get("text"))
                );
                if (StringUtils.hasText(id) && StringUtils.hasText(copy)) {
                    result.put(id.trim(), copy.trim());
                }
            }
        }
        return result;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String str ? str : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
