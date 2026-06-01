package com.marketinghub.geralanding.presetdesign.provisorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private static final Logger log = LoggerFactory.getLogger(DesignPresetProvisionalHtmlProcessor.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Processa wireframe, copy, planejamento de imagem e preset tokenizado para gerar o HTML provisório consolidado. */
    public String process(
            String wireframeJson,
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
        applyFallbackButtonTypes(document);

        /*
         * Fallbacks externos.
         */
        applyCtaUrls(document, copyRoot);
        applyImageUrlsByElementId(document, imagePlanningJson);

        /*
         * CSS e classes do preset tokenizado.
         */
        applyTokenizedPresetStyles(document, designRoot);
        applyTokenizedPresetStyles(document, wireframeRoot);

        validateGeneratedHtml(document, wireframeRoot, designRoot);

        return normalizeSerializedHtml(document.outerHtml());
    }

    /** Interpreta um payload JSON de entrada como mapa para processamento estrutural. */
    private Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar JSON de entrada", e);
        }
    }

    /** Valida se o preset de design usa o contrato tokenizado esperado para CSS e página. */
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
        String rawTag = asString(node.get("tag"));
        String tag = firstNonBlank(rawTag, "section");
        tag = sanitizeTagName(tag);

        Element element;
        try {
            element = document.createElement(tag);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Falha ao criar elemento do wireframe: " + describeNode(node, rawTag), ex);
        }

        String id = asString(node.get("id"));
        if (StringUtils.hasText(id)) {
            element.attr("id", normalizeHtmlId(id));
        }

        /*
         * Coloca classes já no HTML base. Depois, applyStructuredPageData e
         * applyTokenizedPresetStyles podem reforçar/duplicar sem causar problemas,
         * porque appendClasses evita duplicatas.
         */
        try {
            appendClasses(element, collectStyleClasses(node.get("estilos")));

            applyInitialTextFromNode(element, node);
            applyTargetSectionId(element, node);
            applyAsset(element, node);
            applyFieldContract(element, node);
            applyFormContract(element, node);
            applyButtonType(element, node);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Falha ao processar propriedades do elemento: " + describeNode(node, rawTag), ex);
        }

        for (Map<String, Object> child : asList(node.get("elementosSeccao"))) {
            try {
                Element childElement = buildElementFromNode(document, child);
                if (childElement != null) {
                    element.appendChild(childElement);
                }
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Falha ao processar filho de " + describeNode(node, rawTag), ex);
            }
        }

        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            try {
                Element childElement = buildElementFromNode(document, child);
                if (childElement != null) {
                    element.appendChild(childElement);
                }
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Falha ao processar filho interno de " + describeNode(node, rawTag), ex);
            }
        }

        /*
         * Ajustes mínimos de HTML válido.
         */
        try {
            applyTagDefaults(element);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Falha ao aplicar defaults do elemento: " + describeNode(node, rawTag), ex);
        }

        return element;
    }

    /**
     * Descreve o nó atual para mensagens de erro detalhadas durante montagem do HTML.
     */
    private String describeNode(Map<String, Object> node, String rawTag) {
        String nodeId = asString(node.get("id"));
        String nodeTag = firstNonBlank(rawTag, asString(node.get("tag")), "<tag-ausente>");
        return "id=" + firstNonBlank(nodeId, "<id-ausente>") + ", tag=" + nodeTag;
    }

    /** Aplica texto inicial do wireframe somente em elementos que podem receber conteúdo sem destruir filhos. */
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

        if (element.children().isEmpty()) {
            element.text(conteudo.trim());
        }
    }

    /** Aplica ajustes mínimos de HTML válido que independem de contexto visual. */
    private void applyTagDefaults(Element element) {
        String tag = element.tagName().toLowerCase(Locale.ROOT);

        if ("img".equals(tag)) {
            element.empty();
        }
    }

    /** Normaliza nomes de tag inválidos para evitar HTML quebrado vindo do JSON. */
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

    /** Normaliza todos os ids do documento para o formato HTML usado nos seletores internos. */
    private void normalizeAllHtmlIds(Document document) {
        for (Element element : document.getAllElements()) {
            if (StringUtils.hasText(element.id())) {
                element.attr("id", normalizeHtmlId(element.id()));
            }
        }
    }

    /** Reaplica dados estruturais por id, preservando compatibilidade com wireframe e design enriquecido. */
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

    /**
     * Aplica classes declaradas em `pagina.body.classes` ao elemento `<body>`.
     */
    private void applyPageBodyClasses(Document document, Map<String, Object> page) {
        if (document == null || page == null) {
            return;
        }

        Element body = document.body();
        if (body == null) {
            return;
        }

        Map<String, Object> bodyMap = asMap(firstNonNull(page.get("body"), page.get("corpo")));
        if (bodyMap.isEmpty()) {
            return;
        }

        appendClasses(body, collectBodyClasses(bodyMap));
    }

    /**
     * Coleta classes do body aceitando múltiplos formatos de contrato.
     */
    private List<String> collectBodyClasses(Map<String, Object> bodyMap) {
        List<String> classes = new ArrayList<>();

        classes.addAll(collectStyleClasses(bodyMap));

        Object classesNode = firstNonNull(bodyMap.get("classes"), bodyMap.get("classList"));
        classes.addAll(collectStyleClasses(classesNode));
        classes.addAll(readStringList(classesNode));

        Object estilosNode = bodyMap.get("estilos");
        classes.addAll(collectStyleClasses(estilosNode));
        classes.addAll(readStringList(estilosNode));

        return classes;
    }

    /** Aplica dados funcionais e classes de cada nó estrutural encontrado por id. */
    private void applyStructuredNodeDataRecursive(Document document, Map<String, Object> node) {
        String id = asString(node.get("id"));
        Element element = resolveElementById(document, id);

        if (element != null) {
            appendClasses(element, collectStyleClasses(node.get("estilos")));
            applyTargetSectionId(element, node);
            applyAsset(element, node);
            applyFieldContract(element, node);
            applyFormContract(element, node);
            applyButtonType(element, node);
        }

        for (Map<String, Object> child : asList(node.get("elementosSeccao"))) {
            applyStructuredNodeDataRecursive(document, child);
        }
        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            applyStructuredNodeDataRecursive(document, child);
        }
    }

    /** Completa tipos de botões sem contrato após a árvore HTML já ter pais de formulário definidos. */
    private void applyFallbackButtonTypes(Document document) {
        for (Element button : document.select("button")) {
            if (StringUtils.hasText(button.attr("type"))) {
                continue;
            }
            if (button.closest("form") != null && normalizeId(button.id()).contains("submit")) {
                button.attr("type", "submit");
                continue;
            }
            if (button.closest("form") == null) {
                button.attr("type", "button");
            }
        }
    }

    /** Aplica href funcional em links priorizando o contrato de interacao do nó. */
    private void applyTargetSectionId(Element element, Map<String, Object> node) {
        if (!"a".equalsIgnoreCase(element.tagName())) {
            return;
        }

        Map<String, Object> interaction = asMap(node.get("interacao"));
        String target = firstNonBlank(
                asString(interaction.get("hrefEsperado")),
                asString(interaction.get("targetSectionId")),
                asString(node.get("targetSectionId")),
                asString(node.get("href")),
                asString(node.get("url")),
                asString(node.get("ctaUrl"))
        );

        if (StringUtils.hasText(target)) {
            element.attr("href", normalizeHref(target));
        }
    }

    /** Aplica o tipo funcional de botões respeitando contrato explícito e contexto de formulário. */
    private void applyButtonType(Element element, Map<String, Object> node) {
        if (!"button".equalsIgnoreCase(element.tagName())) {
            return;
        }

        String explicitType = firstNonBlank(
                asString(node.get("type")),
                asString(asMap(node.get("contratoBotao")).get("type")),
                asString(asMap(node.get("buttonContract")).get("type"))
        );
        if (StringUtils.hasText(explicitType)) {
            element.attr("type", explicitType.trim().toLowerCase(Locale.ROOT));
            return;
        }

        if (isInsideForm(element) && isSubmitButtonIntent(element, node)) {
            element.attr("type", "submit");
            return;
        }

        if (!isInsideForm(element) && !StringUtils.hasText(element.attr("type"))) {
            element.attr("type", "button");
        }
    }

    /** Verifica se o botão já está inserido dentro de um formulário no DOM montado. */
    private boolean isInsideForm(Element element) {
        return element.closest("form") != null;
    }

    /** Identifica intenção de envio pelo id, pela interação textual ou pelo componente declarado. */
    private boolean isSubmitButtonIntent(Element element, Map<String, Object> node) {
        String id = normalizeId(firstNonBlank(element.id(), asString(node.get("id"))));
        if (id.contains("submit")) {
            return true;
        }

        Map<String, Object> interaction = asMap(node.get("interacao"));
        String actionIntent = normalizeId(asString(interaction.get("intencaoAcao")));
        if (actionIntent.contains("enviar")
                || actionIntent.contains("envio")
                || actionIntent.contains("submit")
                || actionIntent.contains("gerar")) {
            return true;
        }

        String component = firstNonBlank(
                asString(node.get("componente")),
                asString(node.get("component")),
                asString(node.get("tipoComponente"))
        );
        return "buttonprimary".equals(normalizeId(component));
    }

    /** Aplica atributos funcionais de imagem declarados em asset, sem criar placeholder visual. */
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

    /** Aplica contrato funcional de campos de formulário sem inserir texto em elementos void. */
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

    /** Aplica contrato funcional de formulário declarado no JSON. */
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

    /** Aplica atributo textual somente quando o valor existe no contrato. */
    private void applyStringAttr(Element element, String attr, Object value) {
        String str = asString(value);
        if (StringUtils.hasText(str)) {
            element.attr(attr, str.trim());
        }
    }

    /** Aplica atributo numérico ou textual preservando o valor declarado no JSON. */
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

    /** Aplica atributo booleano HTML quando o contrato declara valor verdadeiro. */
    private void applyBooleanAttr(Element element, String attr, Object value) {
        if (Boolean.TRUE.equals(value)) {
            element.attr(attr, attr);
        }
    }

    /** Aplica copy por id sem sobrescrever containers estruturais com filhos. */
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

    /** Direciona copy para texto, placeholder ou alt conforme o tipo de elemento. */
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

    /** Indica quais tags podem ter texto próprio alterado preservando filhos. */
    private boolean canHaveDirectTextBeforeChildren(String tag) {
        return "li".equals(tag)
                || "summary".equals(tag)
                || "label".equals(tag)
                || "button".equals(tag)
                || "a".equals(tag);
    }

    /** Substitui somente os nós de texto próprios do elemento e preserva elementos filhos. */
    private void replaceOwnTextBeforeChildren(Element element, String text) {
        List<TextNode> textNodes = new ArrayList<>(element.textNodes());
        for (TextNode node : textNodes) {
            node.remove();
        }

        element.insertChildren(0, new TextNode(text + " "));
    }

    /** Coleta textos de copy por id aceitando os formatos legados de seções. */
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

    /** Aplica fallback de URLs de CTA apenas quando o link ainda não recebeu href confiável. */
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

    /** Coleta URLs de CTA em mapa heurístico compatível com contratos anteriores. */
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

    /** Coleta URL padrão de CTA para fallback quando houver mapeamento confiável por tipo. */
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

    /** Aplica planejamento externo de imagens por elementId, com prioridade sobre asset do wireframe. */
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

            if (StringUtils.hasText(spec.alt())) {
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

    /** Coleta especificações de imagem indexadas por elementId, nunca por posição. */
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
            Element style = document.head().appendElement("style");
            style.appendText(css);
        }

        Map<String, Object> page = asMap(root.get("pagina"));
        applyPageBodyClasses(document, page);
        applyTokenizedSectionClasses(document, page);
    }

    /** Gera CSS exclusivamente a partir dos tokens declarados em definicoes. */
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

    /** Acrescenta regras CSS declaradas para um viewport sem adicionar defaults visuais. */
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

    /** Aplica classes tokenizadas nas seções declaradas no preset. */
    private void applyTokenizedSectionClasses(Document document, Map<String, Object> page) {
        Map<String, Object> corpo = asMap(page.get("corpo"));
        for (Map<String, Object> sectionMap : asList(corpo.get("secoes"))) {
            applyTokenizedNodeClasses(document, sectionMap);
        }
    }

    /** Aplica classes tokenizadas recursivamente por id de elemento. */
    private void applyTokenizedNodeClasses(Document document, Map<String, Object> node) {
        String id = asString(node.get("id"));
        Element element = resolveElementById(document, id);

        if (element != null) {
            appendClasses(element, collectStyleClasses(node.get("estilos")));
        }

        for (Map<String, Object> child : asList(node.get("elementosSeccao"))) {
            applyTokenizedNodeClasses(document, child);
        }
        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            applyTokenizedNodeClasses(document, child);
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
                    continue;
                }

                String className = asString(item);
                if (StringUtils.hasText(className)) {
                    classes.add(className.trim());
                }
            }
        }

        return classes;
    }

    /** Lê lista textual de classes ignorando valores ausentes ou inválidos. */
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

    /** Acrescenta classes declaradas no JSON sem duplicá-las no elemento. */
    private void appendClasses(Element element, List<String> classes) {
        for (String className : classes) {
            if (StringUtils.hasText(className) && !element.hasClass(className)) {
                element.addClass(className);
            }
        }
    }

    /** Resolve elemento por id bruto ou normalizado para compatibilidade entre contratos. */
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

    /** Executa validações finais de contrato e registra warnings sem contaminar o HTML final. */
    private void validateGeneratedHtml(Document document, Map<String, Object> wireframeRoot, Map<String, Object> designRoot) {
        Set<String> definedTokens = collectDefinedCssTokens(wireframeRoot, designRoot);
        validateUsedCssTokens(document, definedTokens);
        validateFunctionalLinks(document, wireframeRoot, designRoot);
        validateFormControls(document, wireframeRoot, designRoot);
        validateImages(document);
        validateContainerChildrenPreserved(document, wireframeRoot, designRoot);
    }

    /** Coleta tokens CSS declarados em definicoes do wireframe e do preset de design. */
    private Set<String> collectDefinedCssTokens(Map<String, Object>... roots) {
        Set<String> tokens = new LinkedHashSet<>();
        for (Map<String, Object> root : roots) {
            Map<String, Object> definitions = asMap(root.get("definicoes"));
            for (Object block : definitions.values()) {
                collectDefinedCssTokensFromBlock(block, tokens);
            }
        }
        return tokens;
    }

    /** Coleta tokens CSS recursivamente a partir dos itens que declaram nome, atributoCss e valor. */
    private void collectDefinedCssTokensFromBlock(Object block, Set<String> tokens) {
        if (block instanceof Map<?, ?> mapBlock) {
            String token = asString(mapBlock.get("nome"));
            if (StringUtils.hasText(token)) {
                tokens.add(token.trim());
            }
            for (Object value : mapBlock.values()) {
                collectDefinedCssTokensFromBlock(value, tokens);
            }
            return;
        }
        if (block instanceof List<?> listBlock) {
            for (Object item : listBlock) {
                collectDefinedCssTokensFromBlock(item, tokens);
            }
        }
    }

    /** Valida se todas as classes aplicadas correspondem a tokens declarados em definicoes. */
    private void validateUsedCssTokens(Document document, Set<String> definedTokens) {
        for (Element element : document.getAllElements()) {
            for (String className : element.classNames()) {
                if (!definedTokens.contains(className)) {
                    log.warn("Token CSS não definido: {}", className);
                }
            }
        }
    }

    /** Valida links funcionais e destinos internos de âncoras. */
    private void validateFunctionalLinks(Document document, Map<String, Object> wireframeRoot, Map<String, Object> designRoot) {
        Map<String, Map<String, Object>> nodesById = collectNodesById(wireframeRoot, designRoot);
        for (Element link : document.select("a")) {
            Map<String, Object> node = nodesById.get(normalizeId(link.id()));
            Map<String, Object> interaction = node == null ? Map.of() : asMap(node.get("interacao"));
            if (StringUtils.hasText(asString(interaction.get("targetSectionId"))) && !StringUtils.hasText(link.attr("href"))) {
                log.warn("Link com interacao.targetSectionId sem href: {}", link.id());
            }
            if (!StringUtils.hasText(link.attr("href"))) {
                log.warn("Link sem href funcional: {}", link.id());
            }
        }

        for (Element linkedElement : document.select("[href^=#]")) {
            String targetId = normalizeHtmlId(linkedElement.attr("href"));
            if (StringUtils.hasText(targetId) && document.getElementById(targetId) == null) {
                log.warn("Href interno aponta para id inexistente: {}", linkedElement.attr("href"));
            }
        }
    }

    /** Valida botões e campos de formulário contra os contratos funcionais declarados. */
    private void validateFormControls(Document document, Map<String, Object> wireframeRoot, Map<String, Object> designRoot) {
        Map<String, Map<String, Object>> nodesById = collectNodesById(wireframeRoot, designRoot);
        for (Element button : document.select("form button")) {
            if (normalizeId(button.id()).contains("submit") && !"submit".equalsIgnoreCase(button.attr("type"))) {
                log.warn("Botão submit dentro de form sem type=submit: {}", button.id());
            }
        }
        for (Element input : document.select("input")) {
            if (!StringUtils.hasText(input.attr("type"))) {
                log.warn("Input sem type funcional: {}", input.id());
            }
            if (!StringUtils.hasText(input.attr("name"))) {
                log.warn("Input sem name funcional: {}", input.id());
            }
            Map<String, Object> node = nodesById.get(normalizeId(input.id()));
            Map<String, Object> contract = node == null ? Map.of() : asMap(node.get("contratoCampo"));
            if (Boolean.TRUE.equals(contract.get("required")) && !input.hasAttr("required")) {
                log.warn("Input required sem atributo required: {}", input.id());
            }
        }
    }

    /** Valida atributos funcionais obrigatórios de imagens e URLs temporárias de exemplo. */
    private void validateImages(Document document) {
        for (Element img : document.select("img")) {
            if (!StringUtils.hasText(img.attr("src"))) {
                log.warn("Imagem sem src funcional: {}", img.id());
            }
            if (!StringUtils.hasText(img.attr("alt"))) {
                log.warn("Imagem sem alt funcional: {}", img.id());
            }
            if (StringUtils.hasText(img.attr("src")) && img.attr("src").contains("example.com")) {
                log.warn("Imagem usa src temporário example.com: {}", img.id());
            }
            if (!img.childNodes().isEmpty()) {
                log.warn("Imagem contém conteúdo interno inválido: {}", img.id());
            }
        }
    }

    /** Valida se containers declarados com filhos mantiveram filhos no HTML gerado. */
    private void validateContainerChildrenPreserved(Document document, Map<String, Object> wireframeRoot, Map<String, Object> designRoot) {
        for (Map<String, Object> node : collectAllNodes(wireframeRoot, designRoot)) {
            String tag = sanitizeTagName(firstNonBlank(asString(node.get("tag")), "section"));
            if (!isContainerTag(tag)) {
                continue;
            }
            int expectedChildren = asList(node.get("elementosSeccao")).size() + asList(node.get("elementosInternos")).size();
            if (expectedChildren == 0) {
                continue;
            }
            Element element = resolveElementById(document, asString(node.get("id")));
            if (element != null && element.children().size() < expectedChildren) {
                log.warn("Container com filhos perdeu estrutura após aplicação de copy: {}", element.id());
            }
        }
    }

    /** Identifica tags de container que não devem receber copy destrutiva quando possuem filhos. */
    private boolean isContainerTag(String tag) {
        return "div".equals(tag)
                || "section".equals(tag)
                || "ul".equals(tag)
                || "ol".equals(tag)
                || "form".equals(tag)
                || "details".equals(tag);
    }

    /** Coleta nós estruturais indexados por id normalizado, dando preferência aos dados mais recentes. */
    private Map<String, Map<String, Object>> collectNodesById(Map<String, Object>... roots) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> node : collectAllNodes(roots)) {
            String id = normalizeId(asString(node.get("id")));
            if (StringUtils.hasText(id)) {
                result.put(id, node);
            }
        }
        return result;
    }

    /** Coleta todos os nós estruturais de pagina.corpo.secoes em múltiplas raízes JSON. */
    private List<Map<String, Object>> collectAllNodes(Map<String, Object>... roots) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> root : roots) {
            Map<String, Object> page = asMap(root.get("pagina"));
            Map<String, Object> body = asMap(page.get("corpo"));
            for (Map<String, Object> section : asList(body.get("secoes"))) {
                collectAllNodesRecursive(section, result);
            }
        }
        return result;
    }

    /** Percorre recursivamente filhos estruturais mantendo a ordem declarada no JSON. */
    private void collectAllNodesRecursive(Map<String, Object> node, List<Map<String, Object>> result) {
        result.add(node);
        for (Map<String, Object> child : asList(node.get("elementosSeccao"))) {
            collectAllNodesRecursive(child, result);
        }
        for (Map<String, Object> child : asList(node.get("elementosInternos"))) {
            collectAllNodesRecursive(child, result);
        }
    }

    /** Converte valor JSON em mapa tipado quando o formato é compatível. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /** Converte valor JSON em lista de mapas quando o formato é compatível. */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    /** Retorna o primeiro mapa não vazio entre alternativas de contrato. */
    @SafeVarargs
    private final Map<String, Object> firstNonEmptyMap(Map<String, Object>... maps) {
        for (Map<String, Object> map : maps) {
            if (map != null && !map.isEmpty()) {
                return map;
            }
        }
        return Map.of();
    }

    /** Normaliza ids para comparação independente de cerquilha, espaços e caixa. */
    private String normalizeId(String value) {
        return normalizeHtmlId(value);
    }

    /** Normaliza id para emissão no HTML removendo cerquilha e caracteres separadores inconsistentes. */
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

    /** Normaliza href preservando URLs absolutas e convertendo destinos internos em âncoras válidas. */
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

    /** Retorna o primeiro valor não nulo entre alternativas de contrato. */
    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    /** Converte valor JSON em string apenas quando o tipo original é textual. */
    private String asString(Object value) {
        return value instanceof String str ? str : null;
    }

    /** Converte números ou strings de contrato em representação textual de atributo. */
    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return String.valueOf(number);
        }
        return asString(value);
    }

    /** Retorna o primeiro texto com conteúdo útil entre alternativas de contrato. */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }

    /** Remove marcadores auxiliares do serializador sem acrescentar metadados técnicos ao HTML final. */
    private String normalizeSerializedHtml(String html) {
        return html
                .replace("/*<![CDATA[*/", "")
                .replace("/*]]>*/", "")
                .replace(" />", "/>");
    }

    /** Valida nomes de propriedades CSS declaradas antes de emitir regras tokenizadas. */
    private boolean isSafeCssPropertyName(String name) {
        return StringUtils.hasText(name)
                && Pattern.matches("-?[A-Za-z][A-Za-z0-9-]*", name);
    }

    /** Representa atributos funcionais de imagem resolvidos por elementId. */
    private record ImageSpec(String url, String alt, String width, String height) {
    }
}
