package com.marketinghub.geralanding.copy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.wireframe.WireframeHtmlGenerator;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Conjunto exclusivo da etapa LANDING_PAGE_COPY: monta o HTML provisório a partir
 * de wireframe + copy, sem executar responsabilidades de image planning/design preset.
 */
@Component
public class CopyProvisionalHtmlProcessor {

    private static final Logger log = LoggerFactory.getLogger(CopyProvisionalHtmlProcessor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final WireframeHtmlGenerator wireframeHtmlGenerator = new WireframeHtmlGenerator();



    /**
     * Monta o HTML base do wireframe e aplica os textos da etapa de copy.
     */
    public String process(String wireframeJson, String copyJson) {
        log.info("[GeraLanding][CopyProvisionalHtmlProcessor] Entrada wireframeJson: {}", wireframeJson);
        log.info("[GeraLanding][CopyProvisionalHtmlProcessor] Entrada copyJson: {}", copyJson);
        if (!StringUtils.hasText(wireframeJson)) {
            throw new IllegalArgumentException("JSON de wireframe ausente");
        }
        if (!StringUtils.hasText(copyJson)) {
            throw new IllegalArgumentException("JSON de copy ausente");
        }

        Map<String, Object> wireframe = unwrapPayload(parseJsonObject(wireframeJson, "wireframeJson"), "landingPageWireframe");
        Map<String, Object> copy = unwrapPayload(parseJsonObject(copyJson, "copyJson"), "landingPageCopy");

        String baseHtml = buildHtmlFromWireframe(wireframe);
        return process(baseHtml, copy);
    }

    /**
     * Aplica a copy no HTML já montado usando o mapeamento por id de elemento.
     */
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
        Map<String, Element> elementByNormalizedId = indexElementsByNormalizedId(document);
        for (Map.Entry<String, String> entry : copyByItemId.entrySet()) {
            Element element = resolveElementByItemId(document, elementByNormalizedId, entry.getKey());
            if (element != null && StringUtils.hasText(entry.getValue())) {
                applyCopyToElement(element, entry.getValue().trim());
            }
        }

        String title = firstNonBlank(copyByItemId.get("title"), copyByItemId.get("s1-title"), copyByItemId.get("s2-title"));
        if (StringUtils.hasText(title)) {
            document.title(title);
        }
        return normalizeSerializedHtml(document.outerHtml());
    }

    private Element resolveElementByItemId(Document document, Map<String, Element> elementByNormalizedId, String itemId) {
        if (!StringUtils.hasText(itemId)) {
            return null;
        }
        Element directElement = document.getElementById(itemId);
        if (directElement != null) {
            return directElement;
        }
        return elementByNormalizedId.get(normalizeId(itemId));
    }

    private Map<String, Element> indexElementsByNormalizedId(Document document) {
        Map<String, Element> result = new HashMap<>();
        for (Element element : document.getAllElements()) {
            String id = element.id();
            if (!StringUtils.hasText(id)) {
                continue;
            }
            result.putIfAbsent(normalizeId(id), element);
        }
        return result;
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


    private void applyCopyToElement(Element element, String value) {
        String tagName = element.tagName();
        if ("input".equalsIgnoreCase(tagName) || "textarea".equalsIgnoreCase(tagName)) {
            element.attr("placeholder", value);
            return;
        }
        element.text(value);
    }

    private String normalizeSerializedHtml(String html) {
        return html
                .replace("/*<![CDATA[*/", "")
                .replace("/*]]>*/", "")
                .replace(" />", "/>");
    }

    /**
     * Interpreta um JSON cujo elemento raiz esperado é um objeto ({...}).
     */
    private Map<String, Object> parseJsonObject(String json, String payloadLabel) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar " + payloadLabel + ": esperado JSON objeto ({...}) com elementos compatíveis com o contrato.", e);
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
        if (!(rawSections instanceof List<?>)) {
            rawSections = loadSectionsFromPaginaModel(copyJson);
        }
        if (!(rawSections instanceof List<?> sections)) {
            return result;
        }

        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)) {
                continue;
            }
            Object rawItems = firstNonNull(sectionMap.get("items"), sectionMap.get("values"), sectionMap.get("fields"));
            if (!(rawItems instanceof List<?>)) {
                rawItems = sectionMap.get("elementosSeccao");
            }
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
                        asString(itemMap.get("text")),
                        asString(itemMap.get("texto")),
                        extractNestedText(itemMap.get("texto"))
                );
                if (StringUtils.hasText(id) && StringUtils.hasText(copy)) {
                    result.put(normalizeId(id), copy.trim());
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object loadSectionsFromPaginaModel(Map<String, Object> payload) {
        Object paginaObj = payload.get("pagina");
        if (!(paginaObj instanceof Map<?, ?> pagina)) {
            return null;
        }
        Object corpoObj = pagina.get("corpo");
        if (!(corpoObj instanceof Map<?, ?> corpo)) {
            return null;
        }
        return corpo.get("secoes");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapPayload(Map<String, Object> root, String contractKey) {
        Object nested = root.get(contractKey);
        if (nested instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return root;
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

    @SuppressWarnings("unchecked")
    private String extractNestedText(Object rawTexto) {
        if (!(rawTexto instanceof Map<?, ?> textoMap)) {
            return null;
        }
        Object conteudo = textoMap.get("conteudo");
        return conteudo instanceof String str ? str : null;
    }
}
