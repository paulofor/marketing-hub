package com.marketinghub.geralanding.designpreset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
/**
 * Responsável por consolidar o HTML provisório final da etapa de design preset,
 * aplicando copy, URLs de imagens planejadas e preset visual no wireframe base.
 */
public class DesignPresetProvisionalHtmlProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DesignPresetWireframeHtmlGenerator wireframeHtmlGenerator = new DesignPresetWireframeHtmlGenerator();

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

        String baseHtml = wireframeHtmlGenerator.generateFromJson(wireframeJson);
        if (!StringUtils.hasText(baseHtml)) {
            throw new IllegalArgumentException("Falha ao gerar HTML base a partir do wireframe");
        }

        Document document = Jsoup.parse(baseHtml, "", Parser.htmlParser());
        document.outputSettings()
                .prettyPrint(false)
                .charset("utf-8")
                .syntax(Document.OutputSettings.Syntax.html);

        Map<String, Object> copyRoot = parseJson(copyJson);
        Map<String, Object> designRoot = parseJson(designPresetJson);
        validateTokenizedPresetContract(designRoot);

        applyCopy(document, copyRoot);
        applyCtaUrls(document, copyRoot);
        applyImageUrlsByElementId(document, imagePlanningJson);
        applyLegacyPresetStyles(document, designRoot);

        return normalizeSerializedHtml(document.outerHtml());
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar JSON de entrada", e);
        }
    }

    /**
     * Valida o contrato atual da etapa de preset, que agora exige o formato tokenizado `definicoes + pagina`.
     */
    private void validateTokenizedPresetContract(Map<String, Object> designRoot) {
        if (designRoot == null || !designRoot.containsKey("definicoes") || !designRoot.containsKey("pagina")) {
            throw new IllegalArgumentException("JSON de design preset fora do contrato atual: esperado formato tokenizado com `definicoes` e `pagina`");
        }
    }

    private void applyCopy(Document document, Map<String, Object> copyRoot) {
        Map<String, String> copyById = collectCopyByItemId(copyRoot);

        for (Map.Entry<String, String> entry : copyById.entrySet()) {
            Element target = resolveElementById(document, entry.getKey());

            if (target == null || !StringUtils.hasText(entry.getValue())) {
                continue;
            }

            applyCopyToElement(target, entry.getValue().trim());
        }

        String title = firstNonBlank(
                copyById.get(normalizeId("title")),
                copyById.get(normalizeId("el-s1-h1")),
                copyById.get(normalizeId("el-s2-h2"))
        );

        if (StringUtils.hasText(title)) {
            document.title(title);
        }
    }

    private void applyCopyToElement(Element target, String text) {
        String tag = target.tagName().toLowerCase(Locale.ROOT);

        if ("input".equals(tag) || "textarea".equals(tag)) {
            target.attr("placeholder", text);
            return;
        }

        if ("img".equals(tag)) {
            if (!StringUtils.hasText(target.attr("alt"))) {
                target.attr("alt", text);
            }
            return;
        }

        if (target.children().isEmpty()) {
            target.text(text);
            return;
        }

        if (canHaveDirectTextBeforeChildren(tag)) {
            replaceOwnTextBeforeChildren(target, text);
            return;
        }

        // Importante:
        // Não aplicar copy diretamente em containers com filhos,
        // para não destruir form, div, ul, ol, details, section etc.
    }

    private boolean canHaveDirectTextBeforeChildren(String tag) {
        return "li".equals(tag)
                || "summary".equals(tag)
                || "label".equals(tag)
                || "button".equals(tag)
                || "a".equals(tag);
    }

    private void replaceOwnTextBeforeChildren(Element element, String text) {
        List<TextNode> textNodes = new ArrayList<>(element.textNodes());
        for (TextNode node : textNodes) {
            node.remove();
        }

        element.insertChildren(0, new TextNode(text + " "));
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

            Object itemsObj = firstNonNull(
                    sectionMap.get("items"),
                    sectionMap.get("fields"),
                    sectionMap.get("values")
            );

            if (!(itemsObj instanceof List<?> items)) {
                continue;
            }

            for (Object item : items) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }

                String id = firstNonBlank(
                        asString(map.get("id")),
                        asString(map.get("item")),
                        asString(map.get("tagId"))
                );

                String text = firstNonBlank(
                        asString(map.get("texto")),
                        asString(map.get("text")),
                        asString(map.get("copy")),
                        asString(map.get("value"))
                );

                if (StringUtils.hasText(id) && text != null) {
                    result.put(normalizeId(id), text.trim());
                }
            }
        }

        return result;
    }

    private void applyCtaUrls(Document document, Map<String, Object> copyRoot) {
        String defaultCtaUrl = collectDefaultCtaUrl(copyRoot);

        if (!StringUtils.hasText(defaultCtaUrl)) {
            return;
        }

        for (Element link : document.select("a[id*=cta]")) {
            if (!StringUtils.hasText(link.attr("href"))) {
                link.attr("href", defaultCtaUrl);
            }
        }
    }

    private String collectDefaultCtaUrl(Map<String, Object> copyRoot) {
        Object ctaBlocksObj = copyRoot.get("ctaBlocks");

        if (!(ctaBlocksObj instanceof List<?> ctaBlocks)) {
            return null;
        }

        for (Object cta : ctaBlocks) {
            if (!(cta instanceof Map<?, ?> map)) {
                continue;
            }

            String url = asString(map.get("ctaUrl"));
            if (StringUtils.hasText(url)) {
                return url.trim();
            }
        }

        return null;
    }

    private void applyImageUrlsByElementId(Document document, String imagePlanningJson) {
        if (!StringUtils.hasText(imagePlanningJson)) {
            return;
        }

        Map<String, Object> planning = parseJson(imagePlanningJson);
        Map<String, ImageSpec> imageByElementId = collectImagesByElementId(planning);

        for (Map.Entry<String, ImageSpec> entry : imageByElementId.entrySet()) {
            Element img = resolveElementById(document, entry.getKey());

            if (img == null || !"img".equalsIgnoreCase(img.tagName())) {
                continue;
            }

            ImageSpec spec = entry.getValue();

            img.attr("src", spec.url());

            if (StringUtils.hasText(spec.alt()) && !StringUtils.hasText(img.attr("alt"))) {
                img.attr("alt", spec.alt());
            }
        }
    }

    private Map<String, ImageSpec> collectImagesByElementId(Map<String, Object> planning) {
        Map<String, ImageSpec> result = new LinkedHashMap<>();

        Object rawImages = firstNonNull(
                planning.get("images"),
                planning.get("landingPageImagePlanning")
        );

        if (rawImages instanceof Map<?, ?> wrapper) {
            rawImages = wrapper.get("images");
        }

        if (!(rawImages instanceof List<?> images)) {
            return result;
        }

        for (Object item : images) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            String elementId = asString(map.get("elementId"));
            String url = firstNonBlank(
                    asString(map.get("imageUrl")),
                    asString(map.get("url"))
            );

            String alt = firstNonBlank(
                    asString(map.get("imageGoal")),
                    asString(map.get("alt")),
                    asString(map.get("description"))
            );

            if (StringUtils.hasText(elementId) && StringUtils.hasText(url)) {
                result.put(normalizeId(elementId), new ImageSpec(url.trim(), alt));
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void applyDesignPreset(Document document, Map<String, Object> root) {
        Map<String, Object> preset = root;

        Object nested = root.get("landingPageDesignPreset");
        if (nested instanceof Map<?, ?> nestedMap) {
            preset = (Map<String, Object>) nestedMap;
        }

        String presetId = asString(preset.get("presetId"));
        if (StringUtils.hasText(presetId) && document.body() != null) {
            document.body().attr("data-preset-id", presetId.trim());
        }

        applyThemeToBody(document, asMap(preset.get("theme")));
        applySectionPresets(document, asList(preset.get("sectionPresets")));
        applyElementPresets(document, asList(preset.get("elementPresets")));
        appendTokenCss(document, asMap(preset.get("theme")));
        appendRuntimeCss(document, preset);
    }

    private void applyThemeToBody(Document document, Map<String, Object> theme) {
        Element body = document.body();
        if (body == null || theme.isEmpty()) {
            return;
        }

        Map<String, Object> palette = asMap(theme.get("palette"));

        Map<String, String> styles = new LinkedHashMap<>();

        String background = asString(palette.get("background"));
        String textPrimary = asString(palette.get("textPrimary"));

        if (StringUtils.hasText(background)) {
            styles.put("background", background.trim());
        }

        if (StringUtils.hasText(textPrimary)) {
            styles.put("color", textPrimary.trim());
        }

        mergeStyle(body, styles);
    }

    private void applySectionPresets(Document document, List<Map<String, Object>> sectionPresets) {
        for (Map<String, Object> sectionPreset : sectionPresets) {
            String sectionId = asString(sectionPreset.get("sectionId"));
            if (!StringUtils.hasText(sectionId)) {
                continue;
            }

            Element section = resolveElementById(document, sectionId);
            if (section == null) {
                continue;
            }

            Map<String, String> styles = collectNameValueStyles(sectionPreset.get("sectionAttributes"));
            mergeStyle(section, styles);
        }
    }

    private void applyElementPresets(Document document, List<Map<String, Object>> elementPresets) {
        for (Map<String, Object> elementPreset : elementPresets) {
            String elementId = asString(elementPreset.get("elementId"));
            if (!StringUtils.hasText(elementId)) {
                continue;
            }

            Element element = resolveElementById(document, elementId);
            if (element == null) {
                continue;
            }

            Map<String, String> styles = collectNameValueStyles(elementPreset.get("attributes"));
            mergeStyle(element, styles);
        }
    }

    private Map<String, String> collectNameValueStyles(Object attributesObj) {
        Map<String, String> styles = new LinkedHashMap<>();

        if (!(attributesObj instanceof List<?> attributes)) {
            return styles;
        }

        for (Object attribute : attributes) {
            if (!(attribute instanceof Map<?, ?> map)) {
                continue;
            }

            String name = asString(map.get("name"));
            String value = asString(map.get("value"));

            if (!isSafeCssPropertyName(name) || !StringUtils.hasText(value)) {
                continue;
            }

            styles.put(name.trim(), value.trim());
        }

        return styles;
    }

    private void mergeStyle(Element element, Map<String, String> stylesToApply) {
        if (element == null || stylesToApply == null || stylesToApply.isEmpty()) {
            return;
        }

        Map<String, String> merged = parseInlineStyle(element.attr("style"));

        for (Map.Entry<String, String> entry : stylesToApply.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }

        element.attr("style", serializeInlineStyle(merged));
    }

    private Map<String, String> parseInlineStyle(String style) {
        Map<String, String> result = new LinkedHashMap<>();

        if (!StringUtils.hasText(style)) {
            return result;
        }

        String[] declarations = style.split(";");

        for (String declaration : declarations) {
            int colonIndex = declaration.indexOf(':');

            if (colonIndex <= 0) {
                continue;
            }

            String name = declaration.substring(0, colonIndex).trim();
            String value = declaration.substring(colonIndex + 1).trim();

            if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
                result.put(name, value);
            }
        }

        return result;
    }

    private String serializeInlineStyle(Map<String, String> styles) {
        StringBuilder out = new StringBuilder();

        for (Map.Entry<String, String> entry : styles.entrySet()) {
            out.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue())
                    .append(";");
        }

        return out.toString();
    }

    private boolean isSafeCssPropertyName(String name) {
        return StringUtils.hasText(name)
                && Pattern.matches("-?[A-Za-z][A-Za-z0-9-]*", name);
    }

    private void appendTokenCss(Document document, Map<String, Object> theme) {
        if (theme.isEmpty() || document.head() == null) {
            return;
        }

        String tokenCss = buildTokenCss(theme);

        if (StringUtils.hasText(tokenCss)) {
            document.head()
                    .appendElement("style")
                    .attr("id", "lhm-theme-tokens")
                    .text(tokenCss);
        }
    }

    @SuppressWarnings("unchecked")
    private void appendRuntimeCss(Document document, Map<String, Object> preset) {
        if (document.head() == null) {
            return;
        }

        Object runtimeObj = preset.get("lhmRuntime");
        if (!(runtimeObj instanceof Map<?, ?> runtime)) {
            return;
        }

        String baseCss = asString(runtime.get("baseCss"));
        if (StringUtils.hasText(baseCss)) {
            document.head()
                    .appendElement("style")
                    .attr("id", "lhm-base-css")
                    .text(baseCss.trim());
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
        appendVars(css, "accessibility", theme.get("accessibility"));
        appendVars(css, "radius", theme.get("radius"));
        appendVars(css, "shadow", theme.get("shadow"));

        css.append("}\n");

        return css.toString();
    }

    private void appendVars(StringBuilder css, String prefix, Object obj) {
        if (!(obj instanceof Map<?, ?> map)) {
            return;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key) || entry.getValue() == null) {
                continue;
            }

            if (entry.getValue() instanceof Map<?, ?> nested) {
                appendVars(css, prefix + "-" + key, nested);
            } else {
                css.append("--lhm-")
                        .append(prefix)
                        .append("-")
                        .append(key)
                        .append(":")
                        .append(entry.getValue())
                        .append(";\n");
            }
        }
    }

    /**
     * Aplica o formato de preset com `definicoes` + `pagina` (estilos tokenizados por classes CSS).
     */
    @SuppressWarnings("unchecked")
    private void applyLegacyPresetStyles(Document document, Map<String, Object> root) {
        if (root == null || !root.containsKey("definicoes") || !root.containsKey("pagina")) {
            return;
        }
        Map<String, Object> definitions = asMap(root.get("definicoes"));
        String css = buildLegacyCss(definitions);
        if (StringUtils.hasText(css) && document.head() != null) {
            document.head().appendElement("style")
                    .attr("id", "lhm-legacy-design-preset-css")
                    .text(css);
        }

        Map<String, Object> page = asMap(root.get("pagina"));
        applyLegacyBodyClasses(document, page);
        applyLegacySectionClasses(document, page);
    }

    private String buildLegacyCss(Map<String, Object> definitions) {
        StringBuilder css = new StringBuilder();
        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            Object block = entry.getValue();
            if (block instanceof Map<?, ?> mapBlock) {
                appendLegacyCssByViewport(css, asList(mapBlock.get("desktop")), null);
                appendLegacyCssByViewport(css, asList(mapBlock.get("mobile")), "@media (max-width: 768px)");
                continue;
            }
            List<Map<String, Object>> attributes = asList(block);
            for (Map<String, Object> attribute : attributes) {
                appendLegacyCssByViewport(css, asList(attribute.get("desktop")), null);
                appendLegacyCssByViewport(css, asList(attribute.get("mobile")), "@media (max-width: 768px)");
            }
        }
        return css.toString();
    }

    private void appendLegacyCssByViewport(StringBuilder css, List<Map<String, Object>> items, String mediaQuery) {
        if (items.isEmpty()) {
            return;
        }
        StringBuilder block = new StringBuilder();
        for (Map<String, Object> item : items) {
            String className = asString(item.get("nome"));
            String property = asString(item.get("atributoCss"));
            String value = asString(item.get("valor"));
            if (!StringUtils.hasText(className) || !isSafeCssPropertyName(property) || !StringUtils.hasText(value)) {
                continue;
            }
            block.append(".").append(className.trim())
                    .append("{").append(property.trim()).append(":").append(value.trim()).append(";}\n");
        }
        if (!StringUtils.hasText(block.toString())) {
            return;
        }
        if (mediaQuery == null) {
            css.append(block);
            return;
        }
        css.append(mediaQuery).append("{\n").append(block).append("}\n");
    }

    private void applyLegacyBodyClasses(Document document, Map<String, Object> page) {
        Element body = document.body();
        if (body == null) {
            return;
        }
        Map<String, Object> corpo = asMap(page.get("corpo"));
        List<Map<String, Object>> styles = asList(corpo.get("estilos"));
        appendClasses(body, collectViewportClassNames(styles));
    }

    private void applyLegacySectionClasses(Document document, Map<String, Object> page) {
        Map<String, Object> corpo = asMap(page.get("corpo"));
        List<Map<String, Object>> sections = asList(corpo.get("secoes"));
        for (Map<String, Object> sectionMap : sections) {
            applyLegacyNodeClasses(document, sectionMap, "id", "elementosSeccao");
        }
    }

    private void applyLegacyNodeClasses(Document document, Map<String, Object> node, String idField, String childrenField) {
        String id = asString(node.get(idField));
        Element element = resolveElementById(document, id);
        if (element != null) {
            appendClasses(element, collectViewportClassNames(asList(node.get("estilos"))));
        }
        List<Map<String, Object>> children = asList(node.get(childrenField));
        for (Map<String, Object> child : children) {
            applyLegacyNodeClasses(document, child, "id", "elementosInternos");
        }
    }

    private List<String> collectViewportClassNames(List<Map<String, Object>> styles) {
        List<String> classes = new ArrayList<>();
        for (Map<String, Object> styleEntry : styles) {
            classes.addAll(readStringList(styleEntry.get("desktop")));
            classes.addAll(readStringList(styleEntry.get("mobile")));
        }
        return classes;
    }

    private List<String> readStringList(Object value) {
        List<String> list = new ArrayList<>();
        if (!(value instanceof List<?> rawList)) {
            return list;
        }
        for (Object item : rawList) {
            String className = asString(item);
            if (StringUtils.hasText(className)) {
                list.add(className.trim());
            }
        }
        return list;
    }

    private void appendClasses(Element element, List<String> classes) {
        for (String className : classes) {
            if (!element.hasClass(className)) {
                element.addClass(className);
            }
        }
    }

    private Element resolveElementById(Document document, String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }

        Element direct = document.getElementById(id);
        if (direct != null) {
            return direct;
        }

        String normalizedId = normalizeId(id);

        for (Element element : document.getAllElements()) {
            if (normalizeId(element.id()).equals(normalizedId)) {
                return element;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private String normalizeId(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value.trim()
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
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
        return html
                .replace("/*<![CDATA[*/", "")
                .replace("/*]]>*/", "")
                .replace(" />", "/>");
    }

    private record ImageSpec(String url, String alt) {
    }
}
