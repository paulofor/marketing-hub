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
import java.util.Locale;
import java.util.Map;

@Component
public class DesignPresetProvisionalHtmlProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final WireframeHtmlGenerator wireframeHtmlGenerator = new WireframeHtmlGenerator();

    public String process(String wireframeJson,
                          String copyJson,
                          String imagePlanningJson,
                          String designPresetJson) {
        if (!StringUtils.hasText(wireframeJson)) {
            throw new IllegalArgumentException("JSON de wireframe ausente");
        }
        if (!StringUtils.hasText(copyJson)) {
            throw new IllegalArgumentException("JSON de copy ausente");
        }
        if (!StringUtils.hasText(designPresetJson)) {
            throw new IllegalArgumentException("JSON de design preset ausente");
        }

        Document document = Jsoup.parse(wireframeHtmlGenerator.generateFromJson(wireframeJson), "", Parser.htmlParser());
        applyCopy(document, parseJson(copyJson));
        applyImageUrls(document, imagePlanningJson);
        applyDesignPreset(document, designPresetJson);
        return normalizeSerializedHtml(document.outerHtml());
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar JSON de entrada", e);
        }
    }

    private void applyCopy(Document document, Map<String, Object> copyRoot) {
        Map<String, String> copyById = collectCopyByItemId(copyRoot);
        for (Map.Entry<String, String> entry : copyById.entrySet()) {
            Element target = resolveElementById(document, entry.getKey());
            if (target == null || !StringUtils.hasText(entry.getValue())) {
                continue;
            }
            String tag = target.tagName();
            if ("input".equalsIgnoreCase(tag) || "textarea".equalsIgnoreCase(tag)) {
                target.attr("placeholder", entry.getValue().trim());
            } else {
                target.text(entry.getValue().trim());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> collectCopyByItemId(Map<String, Object> copyRoot) {
        Map<String, String> result = new LinkedHashMap<>();
        Object sectionsObj = firstNonNull(copyRoot.get("bodySections"), copyRoot.get("sections"));
        if (!(sectionsObj instanceof List<?> sections)) {
            return result;
        }
        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)) {
                continue;
            }
            Object itemsObj = firstNonNull(sectionMap.get("items"), sectionMap.get("fields"), sectionMap.get("values"));
            if (!(itemsObj instanceof List<?> items)) {
                continue;
            }
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                String id = firstNonBlank(asString(map.get("id")), asString(map.get("item")), asString(map.get("tagId")));
                String text = firstNonBlank(asString(map.get("texto")), asString(map.get("text")), asString(map.get("copy")), asString(map.get("value")));
                if (StringUtils.hasText(id) && StringUtils.hasText(text)) {
                    result.put(normalizeId(id), text.trim());
                }
            }
        }
        return result;
    }

    private Element resolveElementById(Document document, String normalizedId) {
        if (!StringUtils.hasText(normalizedId)) {
            return null;
        }
        Element direct = document.getElementById(normalizedId);
        if (direct != null) {
            return direct;
        }
        for (Element element : document.getAllElements()) {
            if (normalizeId(element.id()).equals(normalizedId)) {
                return element;
            }
        }
        return null;
    }

    private void applyImageUrls(Document document, String imagePlanningJson) {
        if (!StringUtils.hasText(imagePlanningJson)) {
            return;
        }
        Map<String, Object> planning = parseJson(imagePlanningJson);
        Object rawImages = firstNonNull(planning.get("images"), planning.get("landingPageImagePlanning"));
        List<String> urls = new ArrayList<>();
        if (rawImages instanceof List<?> list) {
            collectUrls(list, urls);
        } else if (rawImages instanceof Map<?, ?> wrapper && wrapper.get("images") instanceof List<?> nested) {
            collectUrls(nested, urls);
        }

        int index = 0;
        for (Element img : document.select("img")) {
            if (index >= urls.size()) {
                break;
            }
            img.attr("src", urls.get(index++));
        }
    }

    private void collectUrls(List<?> images, List<String> urls) {
        for (Object item : images) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String url = firstNonBlank(asString(map.get("imageUrl")), asString(map.get("url")));
            if (StringUtils.hasText(url)) {
                urls.add(url.trim());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyDesignPreset(Document document, String designPresetJson) {
        Map<String, Object> root = parseJson(designPresetJson);
        Map<String, Object> preset = root;
        Object nested = root.get("landingPageDesignPreset");
        if (nested instanceof Map<?, ?> nestedMap) {
            preset = (Map<String, Object>) nestedMap;
        }

        String presetId = asString(preset.get("presetId"));
        if (StringUtils.hasText(presetId) && document.body() != null) {
            document.body().attr("data-preset-id", presetId.trim());
        }

        Element head = document.head();
        if (head == null) {
            return;
        }

        Object runtimeObj = preset.get("lhmRuntime");
        if (runtimeObj instanceof Map<?, ?> runtime) {
            String baseCss = asString(runtime.get("baseCss"));
            if (StringUtils.hasText(baseCss)) {
                head.appendElement("style").attr("id", "lhm-base-css").text(baseCss.trim());
            }
        }

        String tokenCss = buildTokenCss((Map<String, Object>) preset.get("theme"));
        if (StringUtils.hasText(tokenCss)) {
            head.appendElement("style").attr("id", "lhm-theme-tokens").text(tokenCss);
        }
    }

    private String buildTokenCss(Map<String, Object> theme) {
        if (theme == null || theme.isEmpty()) {
            return null;
        }
        StringBuilder css = new StringBuilder(":root{\n");
        appendVars(css, "palette", theme.get("palette"));
        appendVars(css, "typography", theme.get("typography"));
        appendVars(css, "spacing", theme.get("spacing"));
        appendVars(css, "radius", theme.get("radius"));
        appendVars(css, "shadow", theme.get("shadow"));
        css.append("}\n");
        css.append("body{color:var(--lhm-palette-text,#111827);background:var(--lhm-palette-background,#ffffff);}");
        return css.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendVars(StringBuilder css, String prefix, Object obj) {
        if (!(obj instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key) || e.getValue() == null) {
                continue;
            }
            if (e.getValue() instanceof Map<?, ?> nested) {
                appendVars(css, prefix + "-" + key, nested);
            } else {
                css.append("--lhm-").append(prefix).append("-").append(key).append(":").append(e.getValue()).append(";\n");
            }
        }
    }

    private String normalizeId(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replace('–', '-').replace('—', '-').replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
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

    private String normalizeSerializedHtml(String html) {
        return html.replace("/*<![CDATA[*/", "").replace("/*]]>*/", "").replace(" />", "/>");
    }
}
