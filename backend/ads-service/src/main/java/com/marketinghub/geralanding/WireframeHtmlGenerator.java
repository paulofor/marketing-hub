package com.marketinghub.geralanding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WireframeHtmlGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String generateFromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        if (json.contains("\"pagina\"") && json.contains("\"corpo\"") && json.contains("\"secoes\"")) {
            return generateFromPaginaJson(json);
        }

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
        html.append("<style>\n");
        html.append(baseCss());

        for (String uiSizes : sizeBlocks) {
            html.append(toCss(uiSizes)).append("\n");
        }

        html.append("</style>\n");
        html.append("</head>\n");

        html.append("<body").append(renderBodyAttributes(bodyAttributes)).append(">\n");

        int sectionIndex = 0;

        for (String tag : tags) {
            String sectionHtml = fillLoremIpsumPlaceholders(tag);
            sectionHtml = applySectionPreviewColor(sectionHtml, sectionIndex++);
            sectionHtml = applyFormPreviewColor(sectionHtml);
            html.append(sectionHtml).append("\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String generateFromPaginaJson(String json) {
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
            Map<String, Object> pagina = asMap(root.get("pagina"));
            Map<String, Object> head = asMap(pagina.get("head"));
            Map<String, Object> corpo = asMap(pagina.get("corpo"));

            StringBuilder html = new StringBuilder();
            html.append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escapeHtmlText(asText(head.get("texto"), "Wireframe provisório"))).append("</title>\n")
                .append("<style>\n").append(baseCss());
            html.append("\nbody{").append(renderInlineStyle(asList(corpo.get("estilos")))).append("}\n");
            html.append("</style>\n</head>\n<body>\n");
            int sectionIndex = 0;
            for (Map<String, Object> secao : asList(corpo.get("secoes"))) {
                html.append(renderSection(secao, sectionIndex++));
            }
            html.append("</body>\n</html>\n");
            return html.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao renderizar novo JSON de wireframe", e);
        }
    }


    private String renderSection(Map<String, Object> secao, int sectionIndex) {
        String sectionHtml = renderElement(secao, "elementosSeccao", "section");
        return applySectionPreviewColor(sectionHtml, sectionIndex);
    }

    private String renderElement(Map<String, Object> node, String childKey, String defaultTag) {
        String tag = asText(node.get("tag"), defaultTag);
        String id = asText(node.get("id"), "");
        String style = renderInlineStyle(asList(node.get("estilos")));
        StringBuilder out = new StringBuilder();
        out.append("<").append(tag);
        if (StringUtils.hasText(id)) out.append(" id=\"").append(escapeHtmlAttribute(id)).append("\"");
        if (StringUtils.hasText(style)) out.append(" style=\"").append(escapeHtmlAttribute(style)).append("\"");
        out.append(">");
        out.append(resolveText(node));
        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            out.append(renderElement(child, "elementosInternos", "div"));
        }
        for (Map<String, Object> child : asList(node.get(childKey))) {
            out.append(renderElement(child, "elementosInternos", "div"));
        }
        out.append("</").append(tag).append(">\n");
        return out.toString();
    }

    private String resolveText(Map<String, Object> node) {
        String tag = asText(node.get("tag"), "").trim().toLowerCase();
        if ("img".equals(tag)) {
            return buildImagePlaceholder(node);
        }

        Map<String, Object> texto = asMap(node.get("texto"));
        String content = asText(texto.get("conteudo"), "").trim();
        return escapeHtmlText(StringUtils.hasText(content) ? content : "");
    }

    private String buildImagePlaceholder(Map<String, Object> node) {
        String style = renderInlineStyle(asList(node.get("estilos")));
        String width = extractCssValue(style, "width", "100%");
        String height = extractCssValue(style, "height", "180px");
        String label = "Imagem " + width + " x " + height;

        return "<div style=\"display:flex;align-items:center;justify-content:center;border:2px dashed #94a3b8;background:#e2e8f0;color:#334155;font-size:12px;width:"
            + escapeHtmlAttribute(width)
            + ";height:"
            + escapeHtmlAttribute(height)
            + ";max-width:100%;\">"
            + escapeHtmlText(label)
            + "</div>";
    }

    private String extractCssValue(String style, String property, String fallback) {
        Pattern pattern = Pattern.compile("(?:^|;)\\s*" + Pattern.quote(property) + "\\s*:\s*([^;]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(style == null ? "" : style);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return fallback;
    }

    private String renderInlineStyle(List<Map<String, Object>> estilos) {
        StringBuilder out = new StringBuilder();
        for (Map<String, Object> estilo : estilos) {
            String nome = asText(estilo.get("nome"), "");
            String valor = asText(estilo.get("valor"), "");
            if (StringUtils.hasText(nome) && StringUtils.hasText(valor)) {
                out.append(nome).append(":").append(valor).append(";");
            }
        }
        return out.toString();
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

    private static String baseCss() {
        return """
            *{box-sizing:border-box;}
            body{margin:0;padding:0;font-family:Arial,sans-serif;line-height:1.4;}
            img{display:block;max-width:100%;}
            section{border-bottom:1px solid #e5e7eb;}
            """;
    }

    private static String fillLoremIpsumPlaceholders(String html) {
        String output = html;

        output = output.replaceAll(
            "(?is)<(h1|h2|h3)([^>]*)>\\s*</\\1>",
            "<$1$2>Lorem ipsum dolor sit amet</$1>"
        );

        output = output.replaceAll(
            "(?is)<p([^>]*)>\\s*</p>",
            "<p$1>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>"
        );

        output = output.replaceAll(
            "(?is)<li([^>]*)>\\s*</li>",
            "<li$1>Lorem ipsum dolor sit amet.</li>"
        );

        output = output.replaceAll(
            "(?is)<a([^>]*)>\\s*</a>",
            "<a$1>Lorem ipsum dolor sit amet</a>"
        );

        output = output.replaceAll(
            "(?is)<button([^>]*)>\\s*</button>",
            "<button$1>Lorem ipsum dolor sit amet</button>"
        );

        output = output.replaceAll(
            "(?is)<label([^>]*)>\\s*</label>",
            "<label$1>Lorem ipsum</label>"
        );

        output = output.replaceAll(
            "(?is)<option([^>]*)>\\s*</option>",
            "<option$1>Lorem ipsum</option>"
        );

        output = output.replaceAll(
            "(?is)<span([^>]*)>\\s*</span>",
            "<span$1>Lorem ipsum</span>"
        );

        output = output.replaceAll(
            "(?is)<summary([^>]*)>\\s*</summary>",
            "<summary$1>Lorem ipsum dolor sit amet</summary>"
        );

        output = output.replaceAll(
            "(?is)<strong([^>]*)>\\s*</strong>",
            "<strong$1>Lorem ipsum</strong>"
        );

        output = output.replaceAll(
            "(?is)<small([^>]*)>\\s*</small>",
            "<small$1>Lorem ipsum</small>"
        );

        return output;
    }

    private static String applySectionPreviewColor(String html, int index) {
        boolean lightSurface = index % 2 == 0;
        String background = lightSurface ? "#ffffff" : "#111111";
        String textColor = lightSurface ? "#111111" : "#ffffff";
        String border = lightSurface ? "#e5e7eb" : "#374151";

        String previewStyle = "background:" + background
            + ";color:" + textColor
            + ";border-bottom:1px solid " + border
            + ";";

        Pattern sectionPattern = Pattern.compile("<section\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
        Matcher sectionMatcher = sectionPattern.matcher(html);

        if (!sectionMatcher.find()) {
            return html;
        }

        String attrs = sectionMatcher.group(1);
        String mergedAttrs = mergeStyleAttribute(attrs, previewStyle);

        return sectionMatcher.replaceFirst(
            Matcher.quoteReplacement("<section" + mergedAttrs + ">")
        );
    }

    private static String applyFormPreviewColor(String html) {
        Pattern formPattern = Pattern.compile("<form\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
        Matcher formMatcher = formPattern.matcher(html);

        if (!formMatcher.find()) {
            return html;
        }

        String attrs = formMatcher.group(1);
        String formStyle = "background:#dcfce7;color:#14532d;border:1px solid #86efac;border-radius:12px;padding:16px;";
        String mergedAttrs = mergeStyleAttribute(attrs, formStyle);

        return formMatcher.replaceFirst(
            Matcher.quoteReplacement("<form" + mergedAttrs + ">")
        );
    }

    private static String mergeStyleAttribute(String attrs, String styleToAppend) {
        Pattern stylePattern = Pattern.compile("\\sstyle=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher styleMatcher = stylePattern.matcher(attrs);

        if (!styleMatcher.find()) {
            return attrs + " style=\"" + styleToAppend + "\"";
        }

        String existingStyle = styleMatcher.group(1).trim();

        String mergedStyle = existingStyle.endsWith(";")
            ? existingStyle + styleToAppend
            : existingStyle + ";" + styleToAppend;

        return styleMatcher.replaceFirst(
            " style=\"" + Matcher.quoteReplacement(mergedStyle) + "\""
        );
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
                int valueEnd = start == '{' ? findMatchingBrace(text, valueStart) : findMatchingBracket(text, valueStart);
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

    private static String escapeHtmlAttribute(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private record BodyAttribute(String name, String value) {
    }
}
