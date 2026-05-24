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

/**
 * Montador completo do HTML provisório final da etapa de design preset.
 *
 * Esta classe NÃO depende mais de DesignPresetWireframeHtmlGenerator.
 *
 * Responsabilidades:
 * 1) gerar HTML base diretamente a partir do wireframe JSON;
 * 2) aplicar copy por ID;
 * 3) aplicar href por targetSectionId;
 * 4) aplicar asset src/alt/width/height em imagens;
 * 5) aplicar contratoCampo/contratoFormulario;
 * 6) gerar CSS a partir de definicoes;
 * 7) aplicar classes vindas de pagina.body e estilos dos nós;
 * 8) normalizar IDs HTML removendo # do atributo id.
 */
@Component
public class DesignPresetProvisionalHtmlProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        Map<String, Object> wireframeRoot = parseJson(wireframeJson);
        Map<String, Object> copyRoot = parseJson(copyJson);
        Map<String, Object> designRoot = parseJson(designPresetJson);

        validateTokenizedPresetContract(designRoot);

        Document document = generateBaseHtmlFromWireframe(wireframeRoot);
        document.outputSettings()
                .prettyPrint(false)
                .charset("utf-8")
                .syntax(Document.OutputSettings.Syntax.html);

        normalizeAllHtmlIds(document);

        /*
         * Conteúdo textual.
         */
        applyCopy(document, copyRoot);

        /*
         * Atributos e semântica vindos do wireframe e/ou design.
         * Em alguns fluxos, o wireframe carrega a árvore estrutural.
         * Em outros, o design preset carrega uma árvore enriquecida.
         */
        applyStructuredPageData(document, wireframeRoot);
        applyStructuredPageData(document, designRoot);

        /*
         * Fallbacks externos.
         */
        applyCtaUrls(document, copyRoot);
        applyImageUrlsByElementId(document, imagePlanningJson);

        /*
         * CSS e classes do preset tokenizado.
         */
        applyTokenizedPresetStyles(document, designRoot);

        return normalizeSerializedHtml(document.outerHtml());
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar JSON de entrada", e);
        }
    }

    private void validateTokenizedPresetContract(Map<String, Object> designRoot) {
        if (designRoot == null || !designRoot.containsKey("definicoes") || !designRoot.containsKey("pagina")) {
            throw new IllegalArgumentException("JSON de design preset fora do contrato atual: esperado formato tokenizado com `definicoes` e `pagina`");
        }
    }

    /**
     * Gera o HTML base diretamente do wireframe.
     */
    private Document generateBaseHtmlFromWireframe(Map<String, Object> wireframeRoot) {
        String skeleton = "<!doctype html><html lang=\"pt-BR\"><head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Wireframe provisório</title>"
                + "</head><body></body></html>";

        Document document = Jsoup.parse(skeleton, "", Parser.htmlParser());

        Map<String, Object> page = asMap(wireframeRoot.get("pagina"));
        Map<String, Object> corpo = asMap(page.get("corpo"));
        List<Map<String, Object>> sections = asList(corpo.get("secoes"));

        Element body = document.body();
        if (body == null) {
            throw new IllegalArgumentException("Falha ao criar body do HTML base");
        }

        for (Map<String, Object> sectionNode : sections) {
            Element section = buildElementFromNode(document, sectionNode);
            if (section != null) {
                body.appendChild(section);
            }
        }

        return document;
    }

    /**
     * Constrói um elemento HTML a partir de um nó do wireframe.
     */
    private Element buildElementFromNode(Document document, Map<String, Object> node) {
        String tag = firstNonBlank(asString(node.get("tag")), "section");
        tag = sanitizeTagName(tag);

        Element element = document.createElement(tag);

        String id = asString(node.get("id"));
        if (StringUtils.hasText(id)) {
            element.attr("id", normalizeHtmlId(id));
        }

        /*
         * Coloca classes já no HTML base. Depois, applyStructuredPageData e
         * applyTokenizedPresetStyles podem reforçar/duplicar sem causar problemas,
         * porque appendClasses evita duplicatas.
         */
        appendClasses(element, collectStyleClasses(node.get("estilos")));

        applyInitialTextFromNode(element, node);
        applyTargetSectionId(element, node);
        applyAsset(element, node);
        applyFieldContract(element, node);
        applyFormContract(element, node);

        for (Map<String, Object> child : asList(node.get("elementosSeccao"))) {
            Element childElement = buildElementFromNode(document, child);
            if (childElement != null) {
                element.appendChild(childElement);
            }
        }

        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            Element childElement = buildElementFromNode(document, child);
            if (childElement != null) {
                element.appendChild(childElement);
            }
        }

        /*
         * Ajustes mínimos de HTML válido.
         */
        applyTagDefaults(element);

        return element;
    }

    private void applyInitialTextFromNode(Element element, Map<String, Object> node) {
        Map<String, Object> texto = asMap(node.get("texto"));
        String conteudo = asString(texto.get("conteudo"));

        if (!StringUtils.hasText(conteudo)) {
            return;
        }

        String tag = element.tagName().toLowerCase(Locale.ROOT);

        if ("input".equals(tag) || "textarea".equals(tag)) {
            element.attr("placeholder", conteudo.trim());
            return;
        }

        if ("img".equals(tag)) {
            if (!StringUtils.hasText(element.attr("alt"))) {
                element.attr("alt", conteudo.trim());
            }
            return;
        }

        element.text(conteudo.trim());
    }

    private void applyTagDefaults(Element element) {
        String tag = element.tagName().toLowerCase(Locale.ROOT);

        if ("button".equals(tag) && !StringUtils.hasText(element.attr("type"))) {
            element.attr("type", "button");
        }

        if ("img".equals(tag) && !StringUtils.hasText(element.attr("alt"))) {
            element.attr("alt", "");
        }
    }

    private String sanitizeTagName(String tag) {
        if (!StringUtils.hasText(tag)) {
            return "div";
        }

        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        if (!Pattern.matches("[a-z][a-z0-9-]*", normalized)) {
            return "div";
        }

        return normalized;
    }

    private void normalizeAllHtmlIds(Document document) {
        for (Element element : document.getAllElements()) {
            if (StringUtils.hasText(element.id())) {
                element.attr("id", normalizeHtmlId(element.id()));
            }
        }
    }

    private void applyStructuredPageData(Document document, Map<String, Object> root) {
        Map<String, Object> page = asMap(root.get("pagina"));
        if (page.isEmpty()) {
            return;
        }

        applyPageBodyClasses(document, page);

        Map<String, Object> corpo = asMap(page.get("corpo"));
        for (Map<String, Object> section : asList(corpo.get("secoes"))) {
            applyStructuredNodeDataRecursive(document, section);
        }
    }

    private void applyStructuredNodeDataRecursive(Document document, Map<String, Object> node) {
        String id = asString(node.get("id"));
        Element element = resolveElementById(document, id);

        if (element != null) {
            appendClasses(element, collectStyleClasses(node.get("estilos")));
            applyTargetSectionId(element, node);
            applyAsset(element, node);
            applyFieldContract(element, node);
            applyFormContract(element, node);
        }

        for (Map<String, Object> child : asList(node.get("elementosSeccao"))) {
            applyStructuredNodeDataRecursive(document, child);
        }
        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            applyStructuredNodeDataRecursive(document, child);
        }
    }

    private void applyTargetSectionId(Element element, Map<String, Object> node) {
        if (!"a".equalsIgnoreCase(element.tagName())) {
            return;
        }

        String target = firstNonBlank(
                asString(node.get("targetSectionId")),
                asString(node.get("href")),
                asString(node.get("url")),
                asString(node.get("ctaUrl"))
        );

        if (StringUtils.hasText(target)) {
            element.attr("href", normalizeHref(target));
        }
    }

    private void applyAsset(Element element, Map<String, Object> node) {
        if (!"img".equalsIgnoreCase(element.tagName())) {
            return;
        }

        Map<String, Object> asset = asMap(node.get("asset"));
        if (asset.isEmpty()) {
            return;
        }

        String src = firstNonBlank(
                asString(asset.get("src")),
                asString(asset.get("url")),
                asString(asset.get("imageUrl"))
        );

        String alt = firstNonBlank(
                asString(asset.get("alt")),
                asString(asset.get("altText")),
                asString(asset.get("description")),
                asString(asset.get("imageGoal"))
        );

        if (StringUtils.hasText(src)) {
            element.attr("src", src.trim());
        }
        if (StringUtils.hasText(alt)) {
            element.attr("alt", alt.trim());
        }

        applyNumericOrStringAttr(element, "width", asset.get("width"));
        applyNumericOrStringAttr(element, "height", asset.get("height"));
    }

    private void applyFieldContract(Element element, Map<String, Object> node) {
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        if (!("input".equals(tag) || "textarea".equals(tag) || "select".equals(tag))) {
            return;
        }

        Map<String, Object> contract = firstNonEmptyMap(
                asMap(node.get("contratoCampo")),
                asMap(node.get("fieldContract")),
                asMap(node.get("inputContract"))
        );

        if (contract.isEmpty()) {
            return;
        }

        applyStringAttr(element, "type", contract.get("type"));
        applyStringAttr(element, "name", contract.get("name"));
        applyStringAttr(element, "autocomplete", contract.get("autocomplete"));
        applyStringAttr(element, "inputmode", contract.get("inputmode"));
        applyStringAttr(element, "pattern", contract.get("pattern"));
        applyStringAttr(element, "minlength", contract.get("minlength"));
        applyStringAttr(element, "maxlength", contract.get("maxlength"));
        applyStringAttr(element, "aria-label", firstNonNull(contract.get("ariaLabel"), contract.get("aria-label")));
        applyStringAttr(element, "aria-describedby", firstNonNull(contract.get("ariaDescribedBy"), contract.get("aria-describedby")));

        String placeholder = firstNonBlank(
                asString(contract.get("placeholder")),
                asString(contract.get("placeholderText"))
        );
        if (StringUtils.hasText(placeholder) && !StringUtils.hasText(element.attr("placeholder"))) {
            element.attr("placeholder", placeholder.trim());
        }

        applyBooleanAttr(element, "required", contract.get("required"));
        applyBooleanAttr(element, "disabled", contract.get("disabled"));
        applyBooleanAttr(element, "readonly", contract.get("readonly"));
    }

    private void applyFormContract(Element element, Map<String, Object> node) {
        if (!"form".equalsIgnoreCase(element.tagName())) {
            return;
        }

        Map<String, Object> contract = firstNonEmptyMap(
                asMap(node.get("contratoFormulario")),
                asMap(node.get("contratoForm")),
                asMap(node.get("formContract"))
        );

        if (contract.isEmpty()) {
            return;
        }

        applyStringAttr(element, "method", contract.get("method"));
        applyStringAttr(element, "action", contract.get("action"));
        applyStringAttr(element, "enctype", contract.get("enctype"));
        applyStringAttr(element, "accept-charset", firstNonNull(contract.get("acceptCharset"), contract.get("accept-charset")));
        applyStringAttr(element, "target", contract.get("target"));

        applyBooleanAttr(element, "novalidate", contract.get("novalidate"));
    }

    private void applyStringAttr(Element element, String attr, Object value) {
        String str = asString(value);
        if (StringUtils.hasText(str)) {
            element.attr(attr, str.trim());
        }
    }

    private void applyNumericOrStringAttr(Element element, String attr, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Number number) {
            element.attr(attr, String.valueOf(number));
            return;
        }

        String str = asString(value);
        if (StringUtils.hasText(str)) {
            element.attr(attr, str.trim());
        }
    }

    private void applyBooleanAttr(Element element, String attr, Object value) {
        if (Boolean.TRUE.equals(value)) {
            element.attr(attr, attr);
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
                copyById.get(normalizeId("hero-headline")),
                copyById.get(normalizeId("hero-h1")),
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

        /*
         * Não aplicar copy diretamente em containers com filhos,
         * para não destruir form, div, ul, ol, details, section etc.
         */
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
        Map<String, String> heuristicUrls = collectCtaUrlsByHeuristic(copyRoot);
        String defaultCtaUrl = collectDefaultCtaUrl(copyRoot);

        for (Element link : document.select("a")) {
            if (StringUtils.hasText(link.attr("href"))) {
                continue;
            }

            String id = normalizeId(link.id());
            String matchedUrl = null;

            for (Map.Entry<String, String> entry : heuristicUrls.entrySet()) {
                if (id.contains(entry.getKey())) {
                    matchedUrl = entry.getValue();
                    break;
                }
            }

            if (!StringUtils.hasText(matchedUrl) && id.contains("cta")) {
                matchedUrl = defaultCtaUrl;
            }

            if (StringUtils.hasText(matchedUrl)) {
                link.attr("href", normalizeHref(matchedUrl));
            }
        }
    }

    private Map<String, String> collectCtaUrlsByHeuristic(Map<String, Object> copyRoot) {
        Map<String, String> result = new LinkedHashMap<>();

        Object ctaBlocksObj = copyRoot.get("ctaBlocks");
        if (!(ctaBlocksObj instanceof List<?> ctaBlocks)) {
            return result;
        }

        for (Object cta : ctaBlocks) {
            if (!(cta instanceof Map<?, ?> map)) {
                continue;
            }

            String placement = normalizeId(asString(map.get("placement")));
            String variant = normalizeId(asString(map.get("ctaVariant")));
            String url = firstNonBlank(
                    asString(map.get("targetSectionId")),
                    asString(map.get("ctaUrl")),
                    asString(map.get("url"))
            );

            if (!StringUtils.hasText(url)) {
                continue;
            }

            if (StringUtils.hasText(placement) && StringUtils.hasText(variant)) {
                result.put(placement + "-" + variant, url.trim());
            }
            if (StringUtils.hasText(variant)) {
                result.put(variant, url.trim());
            }
        }

        return result;
    }

    private String collectDefaultCtaUrl(Map<String, Object> copyRoot) {
        Object ctaBlocksObj = copyRoot.get("ctaBlocks");

        if (!(ctaBlocksObj instanceof List<?> ctaBlocks)) {
            return null;
        }

        String firstUrl = null;

        for (Object cta : ctaBlocks) {
            if (!(cta instanceof Map<?, ?> map)) {
                continue;
            }

            String url = firstNonBlank(
                    asString(map.get("targetSectionId")),
                    asString(map.get("ctaUrl")),
                    asString(map.get("url"))
            );

            if (!StringUtils.hasText(url)) {
                continue;
            }

            if (!StringUtils.hasText(firstUrl)) {
                firstUrl = url.trim();
            }

            String ctaType = normalizeId(asString(map.get("ctaType")));
            if ("conversion".equals(ctaType)) {
                return url.trim();
            }
        }

        return firstUrl;
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

            if (StringUtils.hasText(spec.url())) {
                img.attr("src", spec.url());
            }

            if (StringUtils.hasText(spec.alt()) && !StringUtils.hasText(img.attr("alt"))) {
                img.attr("alt", spec.alt());
            }

            if (StringUtils.hasText(spec.width())) {
                img.attr("width", spec.width());
            }

            if (StringUtils.hasText(spec.height())) {
                img.attr("height", spec.height());
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
                    asString(map.get("url")),
                    asString(map.get("src"))
            );

            String alt = firstNonBlank(
                    asString(map.get("alt")),
                    asString(map.get("altText")),
                    asString(map.get("imageGoal")),
                    asString(map.get("description"))
            );

            String width = valueAsString(map.get("width"));
            String height = valueAsString(map.get("height"));

            if (StringUtils.hasText(elementId) && StringUtils.hasText(url)) {
                result.put(normalizeId(elementId), new ImageSpec(url.trim(), alt, width, height));
            }
        }

        return result;
    }

    /**
     * Aplica o formato de preset com `definicoes` + `pagina`.
     */
    private void applyTokenizedPresetStyles(Document document, Map<String, Object> root) {
        if (root == null || !root.containsKey("definicoes") || !root.containsKey("pagina")) {
            return;
        }

        Map<String, Object> definitions = asMap(root.get("definicoes"));
        String css = buildTokenizedCss(definitions);
        if (StringUtils.hasText(css) && document.head() != null) {
            Element existing = document.getElementById("lhm-legacy-design-preset-css");
            if (existing != null) {
                existing.remove();
            }

            document.head().appendElement("style")
                    .attr("id", "lhm-legacy-design-preset-css")
                    .text(css);
        }

        Map<String, Object> page = asMap(root.get("pagina"));
        applyPageBodyClasses(document, page);
        applyTokenizedSectionClasses(document, page);
    }

    private String buildTokenizedCss(Map<String, Object> definitions) {
        StringBuilder css = new StringBuilder();

        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            Object block = entry.getValue();

            if (block instanceof Map<?, ?> mapBlock) {
                appendTokenizedCssByViewport(css, asList(mapBlock.get("desktop")), null);
                appendTokenizedCssByViewport(css, asList(mapBlock.get("mobile")), "@media (max-width: 768px)");
                continue;
            }

            List<Map<String, Object>> attributes = asList(block);
            for (Map<String, Object> attribute : attributes) {
                appendTokenizedCssByViewport(css, asList(attribute.get("desktop")), null);
                appendTokenizedCssByViewport(css, asList(attribute.get("mobile")), "@media (max-width: 768px)");
            }
        }

        return css.toString();
    }

    private void appendTokenizedCssByViewport(StringBuilder css, List<Map<String, Object>> items, String mediaQuery) {
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

    private void applyTokenizedSectionClasses(Document document, Map<String, Object> page) {
        Map<String, Object> corpo = asMap(page.get("corpo"));
        for (Map<String, Object> sectionMap : asList(corpo.get("secoes"))) {
            applyTokenizedNodeClasses(document, sectionMap, "elementosSeccao");
        }
    }

    private void applyTokenizedNodeClasses(Document document, Map<String, Object> node, String childrenField) {
        String id = asString(node.get("id"));
        Element element = resolveElementById(document, id);

        if (element != null) {
            appendClasses(element, collectStyleClasses(node.get("estilos")));
        }

        for (Map<String, Object> child : asList(node.get(childrenField))) {
            applyTokenizedNodeClasses(document, child, "elementosInternos");
        }
    }

    /**
     * Aceita:
     * 1) estilos: { desktop: [...], mobile: [...] }
     * 2) estilos: [ { desktop: [...], mobile: [...] } ]
     */
    private List<String> collectStyleClasses(Object estilosObj) {
        List<String> classes = new ArrayList<>();

        if (estilosObj instanceof Map<?, ?> map) {
            classes.addAll(readStringList(map.get("desktop")));
            classes.addAll(readStringList(map.get("mobile")));
            return classes;
        }

        if (estilosObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    classes.addAll(readStringList(map.get("desktop")));
                    classes.addAll(readStringList(map.get("mobile")));
                }
            }
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
            if (StringUtils.hasText(className) && !element.hasClass(className)) {
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

    @SafeVarargs
    private final Map<String, Object> firstNonEmptyMap(Map<String, Object>... maps) {
        for (Map<String, Object> map : maps) {
            if (map != null && !map.isEmpty()) {
                return map;
            }
        }
        return Map.of();
    }

    private String normalizeId(String value) {
        return normalizeHtmlId(value);
    }

    private String normalizeHtmlId(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value.trim()
                .replaceFirst("^#", "")
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeHref(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("mailto:")
                || lower.startsWith("tel:")
                || lower.startsWith("/")
                || lower.startsWith("?")) {
            return trimmed;
        }

        if (trimmed.startsWith("#")) {
            return "#" + normalizeHtmlId(trimmed);
        }

        return "#" + normalizeHtmlId(trimmed);
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

    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return String.valueOf(number);
        }
        return asString(value);
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

    private boolean isSafeCssPropertyName(String name) {
        return StringUtils.hasText(name)
                && Pattern.matches("-?[A-Za-z][A-Za-z0-9-]*", name);
    }

    private record ImageSpec(String url, String alt, String width, String height) {
    }
}
