package com.marketinghub.geralanding.designpreset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DesignPresetWireframeHtmlGenerator {
    /**
     * Conjunto exclusivo da etapa LANDING_PAGE_WIREFRAME: renderiza HTML base
     * a partir do artefato JSON de wireframe, sem responsabilidades de copy/imagens/design.
     */

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String generateFromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        if (json.contains("\"pagina\"") && json.contains("\"corpo\"") && json.contains("\"secoes\"")) {
            return generateFromPaginaJson(json);
        }

        return generateFromLegacySectionOrderJson(json);
    }

    private String generateFromPaginaJson(String json) {
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
            Map<String, Object> pagina = asMap(root.get("pagina"));
            Map<String, Object> head = asMap(pagina.get("head"));
            Map<String, Object> corpo = asMap(pagina.get("corpo"));
            Map<String, Object> definicoes = asMap(root.get("definicoes"));

            StringBuilder html = new StringBuilder();

            html.append("<!doctype html>\n");
            html.append("<html lang=\"pt-BR\">\n");
            html.append("<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
            html.append("<title>")
                    .append(escapeHtmlText(asText(head.get("texto"), "Wireframe provisório")))
                    .append("</title>\n");
            String responsiveCss = renderResponsiveCssDefinitions(definicoes);
            if (StringUtils.hasText(responsiveCss)) {
                html.append("<style>\n")
                        .append(responsiveCss)
                        .append("\n</style>\n");
            }
            html.append("</head>\n");

            html.append("<body");

            String bodyStyle = renderInlineStyle(asList(corpo.get("estilos")));
            if (StringUtils.hasText(bodyStyle)) {
                html.append(" style=\"").append(escapeHtmlAttribute(bodyStyle)).append("\"");
            }

            html.append(">\n");

            int sectionIndex = 0;
            for (Map<String, Object> secao : asList(corpo.get("secoes"))) {
                html.append(renderSection(secao, sectionIndex));
                sectionIndex++;
            }

            html.append("</body>\n");
            html.append("</html>\n");

            return html.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao renderizar novo JSON de wireframe", e);
        }
    }

    private String renderSection(Map<String, Object> secao, int sectionIndex) {
        Map<String, Object> normalizedSection = new LinkedHashMap<>(secao);
        List<Map<String, Object>> estilos = new ArrayList<>(asList(secao.get("estilos")));
        if (!containsBackgroundStyle(estilos)) {
            estilos.add(Map.of("nome", "background-color", "valor", sectionIndex % 2 == 0 ? "#FFFFFF" : "#F7F9FC"));
        }
        normalizedSection.put("estilos", estilos);
        return renderNode(normalizedSection, "section", "elementosSeccao");
    }

    private String renderElement(Map<String, Object> node) {
        return renderNode(node, "div", "elementosInternos");
    }

    private String renderNode(Map<String, Object> node, String defaultTag, String childrenKey) {
        String tag = asText(node.get("tag"), defaultTag).trim().toLowerCase();

        if (isVoidElement(tag)) {
            return renderVoidElement(node, tag);
        }

        StringBuilder out = new StringBuilder();

        out.append("<").append(tag);
        appendCommonAttributes(out, node);
        out.append(">");

        out.append(resolveText(node));

        for (Map<String, Object> child : asList(node.get(childrenKey))) {
            out.append(renderElement(child));
        }

        out.append("</").append(tag).append(">\n");

        return out.toString();
    }

    private String renderVoidElement(Map<String, Object> node, String tag) {
        StringBuilder out = new StringBuilder();

        out.append("<").append(tag);
        appendCommonAttributes(out, node);

        if ("img".equals(tag)) {
            if (!hasAttribute(node, "alt")) {
                out.append(" alt=\"\"");
            }
            appendSuggestedImageSize(out, node);
        }

        out.append(">\n");

        return out.toString();
    }

    private void appendCommonAttributes(StringBuilder out, Map<String, Object> node) {
        String id = asText(node.get("id"), "");
        String style = renderInlineStyle(asList(node.get("estilos")));
        String className = renderClassNameFromResponsiveStyleRefs(asList(node.get("estilos")));
        Map<String, Object> attrs = extractAttributes(node);

        if (StringUtils.hasText(id)) {
            out.append(" id=\"").append(escapeHtmlAttribute(id)).append("\"");
        }

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();

            if (!isSafeHtmlAttributeName(name)) {
                continue;
            }

            if ("id".equalsIgnoreCase(name) || "style".equalsIgnoreCase(name)) {
                continue;
            }

            Object rawValue = entry.getValue();

            if (rawValue instanceof Boolean bool) {
                if (bool) {
                    out.append(" ").append(name);
                }
                continue;
            }

            String value = asText(rawValue, "");

            if (StringUtils.hasText(value)) {
                out.append(" ")
                        .append(name)
                        .append("=\"")
                        .append(escapeHtmlAttribute(value))
                        .append("\"");
            }
        }

        if (StringUtils.hasText(style)) {
            out.append(" style=\"").append(escapeHtmlAttribute(style)).append("\"");
        }
        if (StringUtils.hasText(className)) {
            out.append(" class=\"").append(escapeHtmlAttribute(className)).append("\"");
        }
    }

    private boolean hasAttribute(Map<String, Object> node, String attributeName) {
        Map<String, Object> attrs = extractAttributes(node);

        for (String key : attrs.keySet()) {
            if (attributeName.equalsIgnoreCase(key)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, Object> extractAttributes(Map<String, Object> node) {
        Map<String, Object> props = asMap(node.get("props"));
        if (!props.isEmpty()) {
            return props;
        }

        Map<String, Object> atributos = asMap(node.get("atributos"));
        if (!atributos.isEmpty()) {
            return atributos;
        }

        return Map.of();
    }

    private boolean isSafeHtmlAttributeName(String name) {
        return StringUtils.hasText(name)
                && Pattern.matches("[A-Za-z_:][-A-Za-z0-9_:.]*", name);
    }

    private boolean isVoidElement(String tag) {
        return "area".equals(tag)
                || "base".equals(tag)
                || "br".equals(tag)
                || "col".equals(tag)
                || "embed".equals(tag)
                || "hr".equals(tag)
                || "img".equals(tag)
                || "input".equals(tag)
                || "link".equals(tag)
                || "meta".equals(tag)
                || "source".equals(tag)
                || "track".equals(tag)
                || "wbr".equals(tag);
    }

    private String resolveText(Map<String, Object> node) {
        Map<String, Object> texto = asMap(node.get("texto"));
        String content = asText(texto.get("conteudo"), "").trim();

        if (!StringUtils.hasText(content)) {
            String lorem = resolveLoremIpsum(node);
            return StringUtils.hasText(lorem) ? escapeHtmlText(lorem) : "";
        }

        return escapeHtmlText(content);
    }

    private String generateFromLegacySectionOrderJson(String json) {
        String sectionOrder = extractSectionOrderArray(json);
        List<String> tags = extractStringFieldValues(sectionOrder, "uiTags");
        List<String> sizeBlocks = extractFieldRawValues(sectionOrder, "uiSizes");
        List<BodyAttribute> bodyAttributes = extractBodyAttributes(json);

        StringBuilder html = new StringBuilder();

        html.append("<!doctype html>\n");
        html.append("<html lang=\"pt-BR\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>Wireframe provisório</title>\n");

        if (!sizeBlocks.isEmpty()) {
            html.append("<style>\n");

            for (String uiSizes : sizeBlocks) {
                html.append(toCss(uiSizes)).append("\n");
            }

            html.append("</style>\n");
        }

        html.append("</head>\n");
        html.append("<body").append(renderBodyAttributes(bodyAttributes)).append(">\n");

        for (String tag : tags) {
            html.append(tag).append("\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String renderInlineStyle(List<Map<String, Object>> estilos) {
        StringBuilder out = new StringBuilder();

        for (Map<String, Object> estilo : estilos) {
            String nome = asText(estilo.get("nome"), "");
            String valor = asText(estilo.get("valor"), "");

            if (StringUtils.hasText(nome) && StringUtils.hasText(valor)) {
                out.append(nome.trim()).append(":").append(valor.trim()).append(";");
            }
        }

        return out.toString();
    }

    private String renderClassNameFromResponsiveStyleRefs(List<Map<String, Object>> estilos) {
        List<String> classes = new ArrayList<>();
        for (Map<String, Object> estilo : estilos) {
            appendClassRefs(classes, estilo.get("desktop"));
            appendClassRefs(classes, estilo.get("mobile"));
        }
        return String.join(" ", classes);
    }

    @SuppressWarnings("unchecked")
    private void appendClassRefs(List<String> classes, Object refs) {
        if (!(refs instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof String className) || !StringUtils.hasText(className)) {
                continue;
            }
            String normalized = className.trim();
            if (!classes.contains(normalized)) {
                classes.add(normalized);
            }
        }
    }

    private String renderResponsiveCssDefinitions(Map<String, Object> definicoes) {
        if (definicoes.isEmpty()) {
            return "";
        }
        Map<String, Map<String, String>> desktopRules = new LinkedHashMap<>();
        Map<String, Map<String, String>> mobileRules = new LinkedHashMap<>();

        for (Object groupObj : definicoes.values()) {
            Map<String, Object> group = asMap(groupObj);
            collectCssRules(desktopRules, group.get("desktop"));
            collectCssRules(mobileRules, group.get("mobile"));
        }

        String desktopCss = renderCssRules(desktopRules);
        String mobileCss = renderCssRules(mobileRules);
        if (!StringUtils.hasText(desktopCss) && !StringUtils.hasText(mobileCss)) {
            return "";
        }
        if (!StringUtils.hasText(mobileCss)) {
            return desktopCss;
        }
        return desktopCss + "\n@media (max-width: 768px) {\n" + mobileCss + "\n}";
    }

    @SuppressWarnings("unchecked")
    private void collectCssRules(Map<String, Map<String, String>> target, Object entriesObj) {
        if (!(entriesObj instanceof List<?> entries)) {
            return;
        }
        for (Object entryObj : entries) {
            if (!(entryObj instanceof Map<?, ?> entry)) {
                continue;
            }
            String className = asText(entry.get("nome"), "");
            String property = asText(entry.get("atributoCss"), "");
            String value = asText(entry.get("valor"), "");
            if (!StringUtils.hasText(className) || !StringUtils.hasText(property) || !StringUtils.hasText(value)) {
                continue;
            }
            target.computeIfAbsent(className.trim(), ignored -> new LinkedHashMap<>())
                    .put(property.trim(), value.trim());
        }
    }

    private String renderCssRules(Map<String, Map<String, String>> rulesByClass) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> entry : rulesByClass.entrySet()) {
            out.append(".").append(entry.getKey()).append(" {");
            for (Map.Entry<String, String> property : entry.getValue().entrySet()) {
                out.append(property.getKey()).append(":").append(property.getValue()).append(";");
            }
            out.append("}\n");
        }
        return out.toString().trim();
    }


    private boolean containsBackgroundStyle(List<Map<String, Object>> estilos) {
        for (Map<String, Object> estilo : estilos) {
            String nome = asText(estilo.get("nome"), "").trim().toLowerCase();
            if ("background".equals(nome) || "background-color".equals(nome)) {
                return true;
            }
        }
        return false;
    }

    private void appendSuggestedImageSize(StringBuilder out, Map<String, Object> node) {
        Map<String, Object> attrs = extractAttributes(node);
        if (hasAttribute(node, "width") || hasAttribute(node, "height")) {
            return;
        }
        int width = averageDimension(attrs.get("minWidth"), attrs.get("maxWidth"), 960);
        int height = averageDimension(attrs.get("minHeight"), attrs.get("maxHeight"), 540);
        out.append(" width=\"").append(width).append("\"");
        out.append(" height=\"").append(height).append("\"");
    }

    private int averageDimension(Object minValue, Object maxValue, int fallback) {
        Integer min = parseInteger(minValue);
        Integer max = parseInteger(maxValue);
        if (min != null && max != null) {
            return (min + max) / 2;
        }
        if (min != null) {
            return min;
        }
        if (max != null) {
            return max;
        }
        return fallback;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (!(value instanceof String text) || !StringUtils.hasText(text)) {
            return null;
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private String resolveLoremIpsum(Map<String, Object> node) {
        Integer min = parseInteger(node.get("textMinWords"));
        Integer max = parseInteger(node.get("textMaxWords"));
        int words = min != null && max != null ? (min + max) / 2 : 14;
        if (words <= 0) {
            return "";
        }
        String base = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt";
        String[] tokens = base.split(" ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words; i++) {
            if (i > 0) {
                out.append(" ");
            }
            out.append(tokens[i % tokens.length]);
        }
        return out.append(".").toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private String asText(Object value, String fallback) {
        return value instanceof String text && StringUtils.hasText(text) ? text : fallback;
    }

    private static String escapeHtmlText(String value) {
        return escapeHtmlAttribute(value);
    }

    private static String escapeHtmlAttribute(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String extractSectionOrderArray(String json) {
        int keyIndex = json.indexOf("\"sectionOrder\"");

        if (keyIndex < 0) {
            throw new IllegalArgumentException("Campo sectionOrder não encontrado.");
        }

        int open = json.indexOf('[', keyIndex);

        if (open < 0) {
            throw new IllegalArgumentException("Array sectionOrder não encontrado.");
        }

        int depth = 0;
        boolean inString = false;

        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && !isEscaped(json, i)) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;

                    if (depth == 0) {
                        return json.substring(open, i + 1);
                    }
                }
            }
        }

        throw new IllegalArgumentException("Não foi possível extrair sectionOrder.");
    }

    private static List<String> extractStringFieldValues(String text, String field) {
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\""
        );

        Matcher matcher = pattern.matcher(text);
        List<String> values = new ArrayList<>();

        while (matcher.find()) {
            values.add(unescapeJson(matcher.group(1)));
        }

        return values;
    }

    private static List<String> extractFieldRawValues(String text, String field) {
        List<String> values = new ArrayList<>();
        String key = "\"" + field + "\"";

        int searchFrom = 0;

        while (true) {
            int keyIndex = text.indexOf(key, searchFrom);

            if (keyIndex < 0) {
                break;
            }

            int colonIndex = text.indexOf(':', keyIndex + key.length());

            if (colonIndex < 0) {
                break;
            }

            int valueStart = colonIndex + 1;

            while (valueStart < text.length() && Character.isWhitespace(text.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart >= text.length()) {
                break;
            }

            char start = text.charAt(valueStart);

            if (start == '"') {
                int valueEnd = findStringEnd(text, valueStart + 1);
                values.add(unescapeJson(text.substring(valueStart + 1, valueEnd)));
                searchFrom = valueEnd + 1;
                continue;
            }

            if (start == '{' || start == '[') {
                int valueEnd = start == '{'
                        ? findMatchingBrace(text, valueStart)
                        : findMatchingBracket(text, valueStart);

                values.add(text.substring(valueStart, valueEnd + 1));
                searchFrom = valueEnd + 1;
                continue;
            }

            int valueEnd = valueStart;

            while (valueEnd < text.length() && text.charAt(valueEnd) != ',' && text.charAt(valueEnd) != '}') {
                valueEnd++;
            }

            values.add(text.substring(valueStart, valueEnd).trim());
            searchFrom = valueEnd + 1;
        }

        return values;
    }

    private static List<BodyAttribute> extractBodyAttributes(String json) {
        List<BodyAttribute> attributes = new ArrayList<>();

        String bodyAttributesArray = extractOptionalArray(json, "bodyAttributes");

        if (!StringUtils.hasText(bodyAttributesArray)) {
            return attributes;
        }

        Pattern objectPattern = Pattern.compile("\\{([^{}]*)}");
        Matcher objectMatcher = objectPattern.matcher(bodyAttributesArray);

        while (objectMatcher.find()) {
            String object = objectMatcher.group(1);

            String attribute = extractStringFieldValue(object, "attribute");
            String value = extractStringFieldValue(object, "value");

            if (StringUtils.hasText(attribute) && value != null) {
                attributes.add(new BodyAttribute(attribute, value));
            }
        }

        return attributes;
    }

    private static String extractOptionalArray(String json, String field) {
        int keyIndex = json.indexOf("\"" + field + "\"");

        if (keyIndex < 0) {
            return null;
        }

        int open = json.indexOf('[', keyIndex);

        if (open < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;

        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && !isEscaped(json, i)) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;

                    if (depth == 0) {
                        return json.substring(open, i + 1);
                    }
                }
            }
        }

        return null;
    }

    private static String extractStringFieldValue(String objectText, String field) {
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\""
        );

        Matcher matcher = pattern.matcher(objectText);

        if (!matcher.find()) {
            return null;
        }

        return unescapeJson(matcher.group(1));
    }

    private static String toCss(String uiSizes) {
        String value = uiSizes.trim();

        if (!StringUtils.hasText(value)) {
            return "";
        }

        if (looksLikePlainCss(value)) {
            return normalizePlainCssSelectors(value);
        }

        return parseJsonLikeCss(value);
    }

    private static boolean looksLikePlainCss(String value) {
        return value.contains("{")
                && value.contains("}")
                && value.contains(":")
                && value.contains(";");
    }

    private static String normalizePlainCssSelectors(String css) {
        StringBuilder output = new StringBuilder();

        int index = 0;

        while (index < css.length()) {
            int openBrace = css.indexOf('{', index);

            if (openBrace < 0) {
                output.append(css.substring(index));
                break;
            }

            String selector = css.substring(index, openBrace);
            int closeBrace = findMatchingBrace(css, openBrace);
            String declarations = css.substring(openBrace + 1, closeBrace);

            if (selector.trim().startsWith("@media")) {
                output.append(selector)
                        .append("{")
                        .append(normalizePlainCssSelectors(declarations))
                        .append("}");
            } else {
                output.append(normalizeSelectorList(selector))
                        .append("{")
                        .append(declarations)
                        .append("}");
            }

            index = closeBrace + 1;
        }

        return output.toString();
    }

    private static String parseJsonLikeCss(String value) {
        StringBuilder css = new StringBuilder();

        int i = 0;

        while (i < value.length()) {
            if (value.charAt(i) == '"') {
                int selectorEnd = findStringEnd(value, i + 1);
                String selector = value.substring(i + 1, selectorEnd);

                i = value.indexOf('{', selectorEnd);

                if (i < 0) {
                    break;
                }

                int objectEnd = findMatchingBrace(value, i);
                String declarationObject = value.substring(i + 1, objectEnd);

                if (selector.startsWith("@media")) {
                    css.append(selector)
                            .append("{\n")
                            .append(parseNestedRules(declarationObject))
                            .append("}\n");
                } else {
                    css.append(normalizeSelectorList(selector))
                            .append(" {\n")
                            .append(parseDeclarations(declarationObject))
                            .append("}\n");
                }

                i = objectEnd + 1;
            } else {
                i++;
            }
        }

        return css.toString();
    }

    private static String parseNestedRules(String nestedObject) {
        StringBuilder output = new StringBuilder();

        int i = 0;

        while (i < nestedObject.length()) {
            if (nestedObject.charAt(i) == '"') {
                int selectorEnd = findStringEnd(nestedObject, i + 1);
                String selector = nestedObject.substring(i + 1, selectorEnd);

                i = nestedObject.indexOf('{', selectorEnd);

                if (i < 0) {
                    break;
                }

                int objectEnd = findMatchingBrace(nestedObject, i);
                String declarations = nestedObject.substring(i + 1, objectEnd);

                output.append(normalizeSelectorList(selector))
                        .append(" {\n")
                        .append(parseDeclarations(declarations))
                        .append("}\n");

                i = objectEnd + 1;
            } else {
                i++;
            }
        }

        return output.toString();
    }

    private static String parseDeclarations(String declarationsObject) {
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(declarationsObject);

        StringBuilder output = new StringBuilder();

        while (matcher.find()) {
            output.append("  ")
                    .append(toKebabCase(matcher.group(1)))
                    .append(": ")
                    .append(matcher.group(2))
                    .append(";\n");
        }

        return output.toString();
    }

    private static String normalizeSelectorList(String selectorList) {
        String[] selectors = selectorList.split(",");
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < selectors.length; i++) {
            if (i > 0) {
                output.append(",");
            }

            output.append(normalizeSingleSelector(selectors[i]));
        }

        return output.toString();
    }

    private static String normalizeSingleSelector(String selector) {
        String trimmed = selector.trim();

        if (!StringUtils.hasText(trimmed)) {
            return trimmed;
        }

        if (trimmed.startsWith("@")) {
            return trimmed;
        }

        if (
                trimmed.startsWith("#")
                        || trimmed.startsWith(".")
                        || trimmed.startsWith("[")
                        || trimmed.startsWith(":")
        ) {
            return trimmed;
        }

        if (
                trimmed.contains(" ")
                        || trimmed.contains(">")
                        || trimmed.contains("+")
                        || trimmed.contains("~")
        ) {
            return trimmed;
        }

        if (isHtmlTagSelector(trimmed)) {
            return trimmed;
        }

        return "#" + trimmed;
    }

    private static boolean isHtmlTagSelector(String selector) {
        return selector.equals("html")
                || selector.equals("body")
                || selector.equals("main")
                || selector.equals("section")
                || selector.equals("div")
                || selector.equals("form")
                || selector.equals("label")
                || selector.equals("input")
                || selector.equals("select")
                || selector.equals("option")
                || selector.equals("button")
                || selector.equals("a")
                || selector.equals("img")
                || selector.equals("h1")
                || selector.equals("h2")
                || selector.equals("h3")
                || selector.equals("p")
                || selector.equals("ul")
                || selector.equals("ol")
                || selector.equals("li")
                || selector.equals("span")
                || selector.equals("summary")
                || selector.equals("details")
                || selector.equals("strong")
                || selector.equals("small");
    }

    private static String renderBodyAttributes(List<BodyAttribute> attributes) {
        if (attributes.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();

        for (BodyAttribute attribute : attributes) {
            output.append(" ")
                    .append(escapeHtmlAttribute(attribute.name()))
                    .append("=\"")
                    .append(escapeHtmlAttribute(attribute.value()))
                    .append("\"");
        }

        return output.toString();
    }

    private static String unescapeJson(String raw) {
        return raw
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\t", "\t")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private static String toKebabCase(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private static int findStringEnd(String value, int start) {
        for (int i = start; i < value.length(); i++) {
            if (value.charAt(i) == '"' && !isEscaped(value, i)) {
                return i;
            }
        }

        throw new IllegalArgumentException("String inválida.");
    }

    private static int findMatchingBracket(String value, int open) {
        int depth = 0;
        boolean inString = false;

        for (int i = open; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '"' && !isEscaped(value, i)) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;

                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Array inválido.");
    }

    private static int findMatchingBrace(String value, int open) {
        int depth = 0;
        boolean inString = false;

        for (int i = open; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '"' && !isEscaped(value, i)) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;

                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Bloco inválido.");
    }

    private static boolean isEscaped(String value, int index) {
        int backslashes = 0;

        for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
            backslashes++;
        }

        return backslashes % 2 == 1;
    }

    private record BodyAttribute(String name, String value) {
    }
}
