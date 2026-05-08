package com.marketinghub.geralanding;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WireframeHtmlGenerator {

    public String generateFromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        String sectionOrder = extractSectionOrderArray(json);
        List<String> tags = extractStringFieldValues(sectionOrder, "uiTags");
        List<String> sizeBlocks = extractStringFieldValues(sectionOrder, "uiSizes");
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

        for (String tag : tags) {
            html.append(tag).append("\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private static String baseCss() {
        return """
            *{box-sizing:border-box;}
            body{margin:0;padding:0;}
            img{display:block;max-width:100%;}
            """;
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

            output
                .append(normalizeSelectorList(selector))
                .append("{")
                .append(declarations)
                .append("}");

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
                        .append("{\n")
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
                    .append("{\n")
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

        if (trimmed.startsWith("#") || trimmed.startsWith(".") || trimmed.startsWith("[") || trimmed.startsWith(":")) {
            return trimmed;
        }

        if (trimmed.contains(" ") || trimmed.contains(">") || trimmed.contains("+") || trimmed.contains("~")) {
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
            || selector.equals("details");
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
