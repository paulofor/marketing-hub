import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WireframeHtmlGenerator {
    public String generateFromJson(String json) {
        String sectionOrder = extractSectionOrderArray(json);
        List<String> tags = extractStringFieldValues(sectionOrder, "uiTags");
        List<String> sizeBlocks = extractStringFieldValues(sectionOrder, "uiSizes");

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html lang=\"pt-BR\">\n<head>\n<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>Wireframe</title>\n<style>\n");
        html.append("*{box-sizing:border-box;} body{margin:0;font-family:Arial,sans-serif;line-height:1.4;color:#111;} img{display:block;} section{border-bottom:1px solid #eee;}\n");
        html.append("#s1-form{background:#fde68a;color:#1f2937;border:2px solid #f59e0b;} #lead-form{background:#fff7ed;padding:12px;border-radius:10px;}\n");
        for (String sizeJson : sizeBlocks) {
            html.append(toCss(sizeJson)).append("\n");
        }
        html.append("</style>\n</head>\n<body>\n");

        int sectionIndex = 0;
        for (String tag : tags) {
            String sectionHtml = applyAlternatingSectionStyle(tag, sectionIndex++);
            sectionHtml = fillTextPlaceholders(sectionHtml);
            sectionHtml = colorizeImageSlots(sectionHtml);
            html.append(sectionHtml).append("\n");
        }
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    // helper methods (same behavior)
    private static String extractSectionOrderArray(String json) { /*...*/
        int keyIndex = json.indexOf("\"sectionOrder\""); if (keyIndex < 0) throw new IllegalArgumentException("Campo sectionOrder não encontrado.");
        int openBracket = json.indexOf('[', keyIndex); int depth = 0; boolean inString = false;
        for (int i = openBracket; i < json.length(); i++) { char c = json.charAt(i); if (c == '"' && json.charAt(i - 1) != '\\') inString = !inString;
            if (!inString) { if (c == '[') depth++; else if (c == ']') { depth--; if (depth == 0) return json.substring(openBracket, i + 1); } } }
        throw new IllegalArgumentException("Array sectionOrder malformado.");
    }
    private static List<String> extractStringFieldValues(String text, String field) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher m = p.matcher(text); List<String> values = new ArrayList<>(); while (m.find()) values.add(unescapeJson(m.group(1))); return values;
    }
    private static String unescapeJson(String raw) { return raw.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "").replace("\\t", "\t").replace("\\/", "/").replace("\\\\", "\\"); }
    private static String toCss(String uiSizesJson) { String normalized = uiSizesJson.trim(); StringBuilder css = new StringBuilder(); int i = 0; while (i < normalized.length()) { if (normalized.charAt(i) == '"') { int selEnd = findStringEnd(normalized, i + 1); String selector = normalized.substring(i + 1, selEnd); i = normalized.indexOf('{', selEnd); if (i < 0) break; int objEnd = findMatchingBrace(normalized, i); String rulesObj = normalized.substring(i + 1, objEnd); if (selector.startsWith("@media")) css.append(selector).append("{").append(parseNestedRules(rulesObj)).append("}\n"); else css.append(selector).append("{").append(parseDeclarations(rulesObj)).append("}\n"); i = objEnd + 1; } else i++; } return css.toString(); }
    private static String parseNestedRules(String nestedObj) { StringBuilder out = new StringBuilder(); int i = 0; while (i < nestedObj.length()) { if (nestedObj.charAt(i) == '"') { int end = findStringEnd(nestedObj, i + 1); String selector = nestedObj.substring(i + 1, end); i = nestedObj.indexOf('{', end); int objEnd = findMatchingBrace(nestedObj, i); String dec = nestedObj.substring(i + 1, objEnd); out.append(selector).append("{").append(parseDeclarations(dec)).append("}"); i = objEnd + 1; } else i++; } return out.toString(); }
    private static String parseDeclarations(String rulesObj) { Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\""); Matcher m = p.matcher(rulesObj); StringBuilder out = new StringBuilder(); while (m.find()) out.append(toKebabCase(m.group(1))).append(':').append(m.group(2)).append(';'); return out.toString(); }
    private static String toKebabCase(String s) { return s.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(); }
    private static int findStringEnd(String s, int start) { for (int i = start; i < s.length(); i++) if (s.charAt(i) == '"' && s.charAt(i - 1) != '\\') return i; throw new IllegalArgumentException("String malformada"); }
    private static int findMatchingBrace(String s, int open) { int depth = 0; boolean inStr = false; for (int i = open; i < s.length(); i++) { char c = s.charAt(i); if (c == '"' && s.charAt(i - 1) != '\\') inStr = !inStr; if (!inStr) { if (c == '{') depth++; else if (c == '}') { depth--; if (depth == 0) return i; } } } throw new IllegalArgumentException("Objeto malformado"); }
    private static String applyAlternatingSectionStyle(String sectionHtml, int sectionIndex) { String style = (sectionIndex % 2 == 0) ? "background:#0f172a;color:#f8fafc;" : "background:#f8fafc;color:#0f172a;"; return sectionHtml.replaceFirst("<section\\b", "<section style=\\\"" + style + "\\\""); }
    private static String fillTextPlaceholders(String sectionHtml) { String withTitle = sectionHtml.replaceAll("<(h1|h2|h3)([^>]*)></\\1>", "<$1$2>Lorem ipsum dolor sit amet</$1>"); String withParagraph = withTitle.replaceAll("<p([^>]*)></p>", "<p$1>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>"); String withSpan = withParagraph.replaceAll("<span([^>]*)></span>", "<span$1>Lorem ipsum</span>"); String withSummary = withSpan.replaceAll("<summary([^>]*)></summary>", "<summary$1>Lorem ipsum dolor sit amet?</summary>"); String withListItems = withSummary.replaceAll("<li([^>]*)></li>", "<li$1>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</li>"); String withLinks = withListItems.replaceAll("<a([^>]*)></a>", "<a$1 href=\"#lead-form\">Lorem ipsum dolor sit amet</a>"); String withButtons = withLinks.replaceAll("<button([^>]*)></button>", "<button$1 type=\"button\">Lorem ipsum dolor sit amet</button>"); return withButtons.replaceAll("<img([^>]*)/>", "<img$1 alt=\"Lorem ipsum preview\" src=\"https://via.placeholder.com/800x500?text=Lorem+Ipsum\" />"); }
    private static String colorizeImageSlots(String sectionHtml) { String[] palette = {"#dbeafe", "#dcfce7", "#fee2e2", "#ede9fe", "#fef3c7"}; Matcher matcher = Pattern.compile("<img([^>]*)/>").matcher(sectionHtml); StringBuffer out = new StringBuffer(); while (matcher.find()) { String attrs = matcher.group(1); String imageId = extractId(attrs); String bg = palette[Math.abs(imageId.hashCode()) % palette.length]; String replacement = "<img" + attrs + " style=\"background:" + bg + ";border:2px dashed #475569;padding:8px;border-radius:12px;\"/>"; matcher.appendReplacement(out, Matcher.quoteReplacement(replacement)); } matcher.appendTail(out); return out.toString(); }
    private static String extractId(String attrs) { Matcher idMatcher = Pattern.compile("id=\"([^\"]+)\"").matcher(attrs); return idMatcher.find() ? idMatcher.group(1) : "img"; }
}
