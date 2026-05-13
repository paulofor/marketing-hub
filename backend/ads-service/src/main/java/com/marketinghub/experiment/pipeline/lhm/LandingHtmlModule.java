package com.marketinghub.experiment.pipeline.lhm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Landing HTML Module (LHM): consolidates canonical inputs for the LANDING_PAGE_HTML prompt.
 */
@Component
public class LandingHtmlModule {
    private static final Logger log = LoggerFactory.getLogger(LandingHtmlModule.class);

    private final ObjectMapper objectMapper;

    public LandingHtmlModule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String buildPromptV2Inputs(Experiment experiment) {
        if (experiment == null) {
            return "";
        }
        String landingPageWireframe = requireArtifact(experiment.getLandingPageWireframe(), "landingPageWireframe");
        String landingPageCopy = requireArtifact(experiment.getLandingPageCopy(), "landingPageCopy");
        String landingPageDesignPreset = requireArtifact(experiment.getLandingPageDesignPreset(), "landingPageDesignPreset");
        String landingPageImagePlanning = requireArtifact(experiment.getLandingPageImagePlanning(), "landingPageImagePlanning");
        StringBuilder sb = new StringBuilder();
        sb.append("\nPrompt v2 (inputs mínimos para LANDING_PAGE_HTML):\n");
        sb.append("Use apenas os 4 insumos abaixo como fonte de verdade para montar o HTML final.\n");
        appendIfPresent(sb, "1) Wireframe aprovado (JSON)", landingPageWireframe);
        appendIfPresent(sb, "2) Texto da landing aprovado (JSON)", landingPageCopy);
        appendIfPresent(sb, "3) Preset de design aprovado (JSON)", landingPageDesignPreset);
        String imageUrls = summarizeImageUrlsFromPlanning(landingPageImagePlanning);
        if (StringUtils.hasText(imageUrls)) {
            sb.append("4) URLs de imagens aprovadas por seção:\n").append(imageUrls);
        } else {
            appendIfPresent(sb, "4) Planejamento de imagens (JSON)", experiment.getLandingPageImagePlanning());
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public String assembleHtmlDocument(Experiment experiment) {
        if (experiment == null) {
            return "";
        }
        String landingPageWireframe = requireArtifact(experiment.getLandingPageWireframe(), "landingPageWireframe");
        String landingPageCopy = requireArtifact(experiment.getLandingPageCopy(), "landingPageCopy");
        String landingPageDesignPreset = requireArtifact(experiment.getLandingPageDesignPreset(), "landingPageDesignPreset");
        String landingPageImagePlanning = requireArtifact(experiment.getLandingPageImagePlanning(), "landingPageImagePlanning");

        Map<String, Object> wireframeRoot = safeReadObject(landingPageWireframe, "landingPageWireframe");
        Map<String, Object> wireframe = unwrapSectionPayload(wireframeRoot, "landingPageWireframe");
        Map<String, Object> formSpec = wireframe.get("formSpec") instanceof Map<?, ?> form
                ? (Map<String, Object>) form
                : Map.of();
        List<Map<String, Object>> sections = wireframe.get("sectionOrder") instanceof List<?> rawSections
                ? rawSections.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> (Map<String, Object>) item)
                .toList()
                : List.of();

        Map<String, Object> copyRoot = safeReadObject(landingPageCopy, "landingPageCopy");
        Map<String, Object> copy = unwrapSectionPayload(copyRoot, "landingPageCopy");
        Map<String, Object> heroCopy = copy.get("hero") instanceof Map<?, ?> rawHero
                ? (Map<String, Object>) rawHero
                : Map.of();
        List<Map<String, Object>> bodySectionsCopy = copy.get("bodySections") instanceof List<?> rawBodySections
                ? rawBodySections.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> (Map<String, Object>) item)
                .toList()
                : List.of();
        List<Map<String, Object>> ctaBlocksCopy = copy.get("ctaBlocks") instanceof List<?> rawCtaBlocks
                ? rawCtaBlocks.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> (Map<String, Object>) item)
                .toList()
                : List.of();
        List<Map<String, Object>> faqCopy = copy.get("faq") instanceof List<?> rawFaq
                ? rawFaq.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> (Map<String, Object>) item)
                .toList()
                : List.of();
        String pageTitle = firstNonBlank(
                asTrimmedString(copy.get("headline")),
                asTrimmedString(heroCopy.get("headline")),
                asTrimmedString(copy.get("title")),
                asTrimmedString(formSpec.get("title")),
                experiment.getName(),
                "Landing");
        String pageSummary = firstNonBlank(
                asTrimmedString(copy.get("summary")),
                asTrimmedString(copy.get("lead")),
                asTrimmedString(heroCopy.get("supportingCopy")));
        String heroSectionId = resolveHeroSectionId(sections);
        String heroHeadline = resolveHeroHeadline(copy, heroCopy, pageTitle);
        String sanitizedPageSummary = sanitizePageSummary(pageSummary, heroHeadline, heroCopy);

        List<Map<String, Object>> plannedImages = extractImages(landingPageImagePlanning);
        log.info("LHM assemble start (experimentId={}): sections={}, plannedImages={}",
                experiment.getId(), sections.size(), plannedImages.size());
        Map<String, Object> designRoot = safeReadObject(landingPageDesignPreset, "landingPageDesignPreset");
        Map<String, Object> designPreset = unwrapSectionPayload(designRoot, "landingPageDesignPreset");
        Map<String, Object> palette = extractPalette(designPreset);
        Map<String, Object> typography = extractTypography(designPreset);
        Map<String, SectionVisualPreset> sectionVisualPresets = extractSectionVisualPresets(designPreset);

        String formId = firstNonBlank(asTrimmedString(formSpec.get("formId")), "lead-capture-primary");
        String submitTarget = firstNonBlank(asTrimmedString(formSpec.get("submitTarget")), "/api/flows/submissions");
        String submitLabel = firstNonBlank(asTrimmedString(formSpec.get("submitLabel")), "Enviar");

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"pt-BR\"><head>")
                .append("<meta charset=\"UTF-8\" />")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\" />")
                .append("<title>").append(escapeHtml(pageTitle)).append("</title>")
                .append(buildBaseCss(designPreset))
                .append("</head><body><main>");

        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            Map<String, Object> section = sections.get(sectionIndex);
            String sectionId = firstNonBlank(asTrimmedString(section.get("sectionId")), "section");
            String sectionName = firstNonBlank(asTrimmedString(section.get("sectionName")), sectionId);
            String sectionObjective = firstNonBlank(
                    asTrimmedString(section.get("objective")),
                    asTrimmedString(section.get("uiNotes")));
            Map<String, Object> surface = section.get("surfaceSpec") instanceof Map<?, ?> rawSurface
                    ? (Map<String, Object>) rawSurface
                    : Map.of();
            SectionVisualPreset visualPreset = sectionVisualPresets.get(normalizeLookupKey(sectionId));
            String surfaceToken = firstNonBlank(asTrimmedString(surface.get("surfaceToken")), "surface-base");
            String surfaceStyle = firstNonBlank(
                    visualPreset != null ? visualPreset.surfaceStyle() : null,
                    asTrimmedString(surface.get("style")),
                    "band");
            String surfaceContrast = firstNonBlank(
                    visualPreset != null ? visualPreset.contrastMode() : null,
                    asTrimmedString(surface.get("contrastMode")),
                    "normal");
            String surfaceStyleClass = "lhm-surface-" + normalizeCssToken(surfaceStyle);
            String surfaceContrastClass = "lhm-" + normalizeCssToken(surfaceContrast);
            boolean isHeroSection = StringUtils.hasText(heroSectionId) && heroSectionId.equalsIgnoreCase(sectionId);

            html.append("<section class=\"lhm-card ")
                    .append(escapeAttr(surfaceStyleClass))
                    .append(" ")
                    .append(escapeAttr(surfaceContrastClass))
                    .append("\" data-section-id=\"")
                    .append(escapeAttr(sectionId))
                    .append("\" data-surface-token=\"").append(escapeAttr(surfaceToken))
                    .append("\" data-surface-style=\"").append(escapeAttr(surfaceStyle))
                    .append("\" data-surface-contrast=\"").append(escapeAttr(surfaceContrast))
                    .append("\">")
                    .append(isHeroSection
                            ? "<h1>" + escapeHtml(heroHeadline) + "</h1>"
                            : "<h2>" + escapeHtml(sectionName) + "</h2>");
            if (isHeroSection && StringUtils.hasText(sanitizedPageSummary)) {
                html.append("<p>").append(escapeHtml(sanitizedPageSummary)).append("</p>");
            }
            if (isHeroSection) {
                html.append(buildHeroCopyMarkup(heroCopy, heroHeadline, submitLabel, submitTarget));
            }
            if (!isHeroSection && StringUtils.hasText(sectionObjective)) {
                html.append("<p class=\"section-objective\">").append(escapeHtml(sectionObjective)).append("</p>");
            }
            html.append(buildBodySectionCopyMarkup(section, bodySectionsCopy));
            html.append(buildFaqMarkup(sectionId, faqCopy));
            html.append(buildCtaBlocksMarkup(sectionId, ctaBlocksCopy));

            int sectionImageCount = 0;
            for (Map<String, Object> image : plannedImages) {
                String imageSectionId = asTrimmedString(image.get("sectionId"));
                if (!sectionId.equalsIgnoreCase(firstNonBlank(imageSectionId, ""))) {
                    continue;
                }
                sectionImageCount++;
                html.append(buildImageTag(image));
            }
            log.info("LHM section rendered (experimentId={}, sectionId={}): matchedImages={}",
                    experiment.getId(), sectionId, sectionImageCount);

            if (containsFormBlock(sectionId, section)) {
                html.append(buildFormMarkup(formId, submitTarget, submitLabel, formSpec));
            }
            html.append("</section>");
        }

        html.append("</main>").append(buildSubmissionScript(formId)).append("</body></html>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildBaseCss(Map<String, Object> designPreset) {
        String cssFromContract = null;
        if (designPreset != null) {
            cssFromContract = asTrimmedString(designPreset.get("baseCss"));
            if (!StringUtils.hasText(cssFromContract) && designPreset.get("lhmRuntime") instanceof Map<?, ?> runtimeMap) {
                cssFromContract = asTrimmedString(((Map<String, Object>) runtimeMap).get("baseCss"));
            }
        }
        if (StringUtils.hasText(cssFromContract)) {
            return "<style>" + cssFromContract + "</style>";
        }
        throw new IllegalStateException("landingPageDesignPreset.lhmRuntime.baseCss (ou baseCss) é obrigatório para renderização LHM.");
    }

    @SuppressWarnings("unchecked")
    private String summarizeImageUrlsFromPlanning(String imagePlanningPayload) {
        Map<String, Object> root = safeReadObject(imagePlanningPayload, "landingPageImagePlanning");
        Map<String, Object> payload = unwrapSectionPayload(root, "landingPageImagePlanning");
        if (!(payload.get("images") instanceof List<?> rawImages)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Object rawImage : rawImages) {
            if (!(rawImage instanceof Map<?, ?> rawImageMap)) {
                continue;
            }
            Map<String, Object> image = (Map<String, Object>) rawImageMap;
            String sectionId = asTrimmedString(image.get("sectionId"));
            String bindingKey = asTrimmedString(image.get("imageBindingKey"));
            String url = firstNonBlank(
                    asTrimmedString(image.get("webUrl")),
                    asTrimmedString(image.get("imageUrl")),
                    asTrimmedString(image.get("sourceUrl")),
                    asTrimmedString(image.get("url")));
            if (!StringUtils.hasText(url)) {
                continue;
            }
            index++;
            sb.append("- #").append(index)
                    .append(" | sectionId=").append(StringUtils.hasText(sectionId) ? sectionId : "(sem sectionId)")
                    .append(" | imageBindingKey=").append(StringUtils.hasText(bindingKey) ? bindingKey : "(sem binding)")
                    .append(" | url=").append(url)
                    .append("\n");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapSectionPayload(Map<String, Object> root, String key) {
        if (root == null || !StringUtils.hasText(key)) {
            return Map.of();
        }
        Object nested = root.get(key);
        if (nested instanceof Map<?, ?> nestedMap) {
            return (Map<String, Object>) nestedMap;
        }
        return root;
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(label).append(":\n").append(value.trim()).append("\n");
        }
    }

    private String asTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> safeReadObject(String raw, String artifactKey) {
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Artefato %s contém JSON inválido.".formatted(artifactKey), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractImages(String raw) {
        Map<String, Object> root = safeReadObject(raw, "landingPageImagePlanning");
        Map<String, Object> payload = unwrapSectionPayload(root, "landingPageImagePlanning");
        if (!(payload.get("images") instanceof List<?> rawImages)) {
            return List.of();
        }
        return rawImages.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String requireArtifact(String raw, String artifactKey) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("Artefato obrigatório ausente: %s.".formatted(artifactKey));
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPalette(Map<String, Object> designPreset) {
        if (designPreset == null || designPreset.isEmpty()) {
            return Map.of();
        }
        if (!(designPreset.get("theme") instanceof Map<?, ?> rawTheme)) {
            return Map.of();
        }
        Map<String, Object> theme = (Map<String, Object>) rawTheme;
        if (!(theme.get("palette") instanceof Map<?, ?> rawPalette)) {
            return Map.of();
        }
        return (Map<String, Object>) rawPalette;
    }

    @SuppressWarnings("unchecked")
    private Map<String, SectionVisualPreset> extractSectionVisualPresets(Map<String, Object> designPreset) {
        if (designPreset == null || !(designPreset.get("sectionPresets") instanceof List<?> rawSectionPresets)) {
            return Map.of();
        }
        return rawSectionPresets.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> (Map<String, Object>) item)
                .map(this::toSectionVisualPreset)
                .filter(preset -> preset != null && StringUtils.hasText(preset.sectionId()))
                .collect(Collectors.toMap(
                        preset -> normalizeLookupKey(preset.sectionId()),
                        preset -> preset,
                        (first, second) -> second));
    }

    private SectionVisualPreset toSectionVisualPreset(Map<String, Object> sectionPreset) {
        if (sectionPreset == null) {
            return null;
        }
        String sectionId = asTrimmedString(sectionPreset.get("sectionId"));
        if (!StringUtils.hasText(sectionId)) {
            return null;
        }
        return new SectionVisualPreset(
                sectionId,
                asTrimmedString(sectionPreset.get("surfaceStyle")),
                asTrimmedString(sectionPreset.get("contrastMode")));
    }

    private String normalizeLookupKey(String value) {
        return firstNonBlank(value, "").trim().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private String buildFormMarkup(String formId,
                                   String submitTarget,
                                   String submitLabel,
                                   Map<String, Object> formSpec) {
        StringBuilder sb = new StringBuilder();
        sb.append("<form id=\"").append(escapeAttr(formId)).append("\" method=\"post\" action=\"")
                .append(escapeAttr(submitTarget)).append("\">");
        if (formSpec.get("fields") instanceof List<?> rawFields) {
            for (Object rawField : rawFields) {
                if (!(rawField instanceof Map<?, ?> rawFieldMap)) {
                    continue;
                }
                Map<String, Object> field = (Map<String, Object>) rawFieldMap;
                String name = firstNonBlank(asTrimmedString(field.get("name")), "campo");
                String type = firstNonBlank(asTrimmedString(field.get("type")), "text");
                String label = firstNonBlank(asTrimmedString(field.get("label")), name);
                String placeholder = firstNonBlank(asTrimmedString(field.get("placeholder")), "");
                boolean required = field.get("required") instanceof Boolean req && req;
                sb.append("<label for=\"field_").append(escapeAttr(name)).append("\">")
                        .append(escapeHtml(label))
                        .append(required ? " *" : "")
                        .append("</label>")
                        .append("<input id=\"field_").append(escapeAttr(name))
                        .append("\" name=\"").append(escapeAttr(name))
                        .append("\" type=\"").append(escapeAttr(type))
                        .append("\" placeholder=\"").append(escapeAttr(placeholder)).append("\"")
                        .append(required ? " required" : "")
                        .append(" />");
            }
        }
        sb.append("<button type=\"submit\">").append(escapeHtml(submitLabel)).append("</button>")
                .append("<p id=\"form-feedback\" role=\"status\"></p>")
                .append("</form>");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTypography(Map<String, Object> designPreset) {
        if (designPreset == null || designPreset.isEmpty()) {
            return Map.of();
        }
        if (!(designPreset.get("theme") instanceof Map<?, ?> rawTheme)) {
            return Map.of();
        }
        Map<String, Object> theme = (Map<String, Object>) rawTheme;
        if (!(theme.get("typography") instanceof Map<?, ?> rawTypography)) {
            return Map.of();
        }
        return (Map<String, Object>) rawTypography;
    }

    private String buildImageTag(Map<String, Object> image) {
        String sectionId = asTrimmedString(image.get("sectionId"));
        if (!StringUtils.hasText(sectionId)) {
            log.warn("LHM image descartada: sectionId ausente. payload={}", summarizeImagePayload(image));
            throw new IllegalStateException("landingPageImagePlanning.images[].sectionId é obrigatório para renderização LHM canônica.");
        }
        String bindingKey = slugifyBindingKey(asTrimmedString(image.get("imageBindingKey")));
        if (!StringUtils.hasText(bindingKey)) {
            log.warn("LHM image descartada: imageBindingKey ausente/inválido (sectionId={}). payload={}",
                    sectionId, summarizeImagePayload(image));
            throw new IllegalStateException("landingPageImagePlanning.images[].imageBindingKey é obrigatório para renderização LHM canônica.");
        }
        String imageRole = firstNonBlank(asTrimmedString(image.get("imageRole")), "image");
        String conversionRole = firstNonBlank(asTrimmedString(image.get("conversionRole")), "support");
        String attentionPriority = firstNonBlank(asTrimmedString(image.get("attentionPriority")), "medium");
        String visualWeight = firstNonBlank(asTrimmedString(image.get("visualWeight")), "secondary");
        String distanceToCta = firstNonBlank(asTrimmedString(image.get("distanceToCTA")), "medium");
        boolean supportsFormConversion = image.get("supportsFormConversion") instanceof Boolean flag && flag;
        String altText = firstNonBlank(asTrimmedString(image.get("sectionName")), imageRole);
        String url = firstNonBlank(
                asTrimmedString(image.get("webUrl")),
                asTrimmedString(image.get("imageUrl")),
                asTrimmedString(image.get("sourceUrl")),
                asTrimmedString(image.get("url")),
                "https://images.unsplash.com/photo-1517836357463-d25dfeac3438");
        log.info("LHM image binding resolved (sectionId={}, bindingKey={}): url={}", sectionId, bindingKey, url);

        return "<img src=\"" + escapeAttr(url) + "\" alt=\"" + escapeAttr(altText)
                + "\" data-image-section-id=\"" + escapeAttr(sectionId)
                + "\" data-image-binding-key=\"" + escapeAttr(bindingKey)
                + "\" data-image-role=\"" + escapeAttr(imageRole)
                + "\" data-conversion-role=\"" + escapeAttr(conversionRole)
                + "\" data-attention-priority=\"" + escapeAttr(attentionPriority)
                + "\" data-visual-weight=\"" + escapeAttr(visualWeight)
                + "\" data-distance-to-cta=\"" + escapeAttr(distanceToCta)
                + "\" data-supports-form-conversion=\"" + supportsFormConversion + "\" />";
    }

    private String summarizeImagePayload(Map<String, Object> image) {
        if (image == null || image.isEmpty()) {
            return "{}";
        }
        return "{sectionId=" + firstNonBlank(asTrimmedString(image.get("sectionId")), "(vazio)")
                + ", imageBindingKey=" + firstNonBlank(asTrimmedString(image.get("imageBindingKey")), "(vazio)")
                + ", webUrl=" + firstNonBlank(asTrimmedString(image.get("webUrl")), "(vazio)")
                + ", imageUrl=" + firstNonBlank(asTrimmedString(image.get("imageUrl")), "(vazio)")
                + ", sourceUrl=" + firstNonBlank(asTrimmedString(image.get("sourceUrl")), "(vazio)")
                + ", url=" + firstNonBlank(asTrimmedString(image.get("url")), "(vazio)")
                + "}";
    }

    private String slugifyBindingKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).replaceAll("-+$", "");
        }
        return normalized.length() >= 3 ? normalized : "";
    }

    private String buildHeroCopyMarkup(Map<String, Object> heroCopy,
                                       String heroHeadline,
                                       String fallbackSubmitLabel,
                                       String fallbackSubmitTarget) {
        if (heroCopy == null || heroCopy.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String promise = asTrimmedString(heroCopy.get("promise"));
        String supportingCopy = asTrimmedString(heroCopy.get("supportingCopy"));
        String proofBadge = asTrimmedString(heroCopy.get("proofBadge"));
        String microcopy = asTrimmedString(heroCopy.get("microcopy"));
        String ctaLabel = firstNonBlank(asTrimmedString(heroCopy.get("ctaLabel")), fallbackSubmitLabel);
        String ctaUrl = firstNonBlank(asTrimmedString(heroCopy.get("ctaUrl")), fallbackSubmitTarget);
        if (StringUtils.hasText(promise) && !isNearDuplicate(promise, heroHeadline)) {
            sb.append("<p class=\"section-objective\">").append(escapeHtml(promise)).append("</p>");
        }
        if (StringUtils.hasText(supportingCopy)) {
            sb.append("<p>").append(escapeHtml(supportingCopy)).append("</p>");
        }
        if (StringUtils.hasText(proofBadge)) {
            sb.append("<p><strong>").append(escapeHtml(proofBadge)).append("</strong></p>");
        }
        if (StringUtils.hasText(microcopy)) {
            sb.append("<p>").append(escapeHtml(microcopy)).append("</p>");
        }
        if (StringUtils.hasText(ctaLabel) && StringUtils.hasText(ctaUrl)) {
            sb.append("<p><a class=\"hero-cta-link\" href=\"")
                    .append(escapeAttr(ctaUrl))
                    .append("\" target=\"_blank\" rel=\"noopener noreferrer\">")
                    .append(escapeHtml(ctaLabel))
                    .append("</a></p>");
        }
        return sb.toString();
    }

    private String resolveHeroSectionId(List<Map<String, Object>> sections) {
        if (sections == null || sections.isEmpty()) {
            return null;
        }
        for (Map<String, Object> section : sections) {
            String sectionId = asTrimmedString(section.get("sectionId"));
            if (StringUtils.hasText(sectionId) && sectionId.toLowerCase(Locale.ROOT).contains("hero")) {
                return sectionId;
            }
        }
        for (Map<String, Object> section : sections) {
            String contentType = asTrimmedString(section.get("contentType"));
            String sectionId = asTrimmedString(section.get("sectionId"));
            if ("hero".equalsIgnoreCase(contentType) && StringUtils.hasText(sectionId)) {
                return sectionId;
            }
        }
        return asTrimmedString(sections.get(0).get("sectionId"));
    }

    private String resolveHeroHeadline(Map<String, Object> copy, Map<String, Object> heroCopy, String fallbackHeadline) {
        String heroHeadline = firstNonBlank(
                asTrimmedString(copy.get("headline")),
                asTrimmedString(heroCopy.get("headline")),
                asTrimmedString(heroCopy.get("subheadline")),
                asTrimmedString(heroCopy.get("promise")),
                fallbackHeadline);
        return compactHeadline(heroHeadline, 16);
    }

    private String compactHeadline(String text, int maxWords) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) {
            return text.trim();
        }
        return String.join(" ", java.util.Arrays.copyOf(words, maxWords)).trim() + "…";
    }

    private String sanitizePageSummary(String pageSummary, String heroHeadline, Map<String, Object> heroCopy) {
        if (!StringUtils.hasText(pageSummary)) {
            return null;
        }
        String supportingCopy = heroCopy == null ? null : asTrimmedString(heroCopy.get("supportingCopy"));
        String promise = heroCopy == null ? null : asTrimmedString(heroCopy.get("promise"));
        if (isNearDuplicate(pageSummary, heroHeadline) || isNearDuplicate(pageSummary, supportingCopy) || isNearDuplicate(pageSummary, promise)) {
            return null;
        }
        return pageSummary.trim();
    }

    private boolean isNearDuplicate(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        String leftNormalized = normalizeTextForComparison(left);
        String rightNormalized = normalizeTextForComparison(right);
        if (Objects.equals(leftNormalized, rightNormalized)) {
            return true;
        }
        if (leftNormalized.contains(rightNormalized) || rightNormalized.contains(leftNormalized)) {
            return true;
        }
        return tokenOverlapRatio(leftNormalized, rightNormalized) >= 0.72d;
    }

    private String normalizeTextForComparison(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double tokenOverlapRatio(String leftNormalized, String rightNormalized) {
        List<String> leftTokens = List.of(leftNormalized.split(" "));
        List<String> rightTokens = List.of(rightNormalized.split(" "));
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        long intersection = leftTokens.stream()
                .distinct()
                .filter(rightTokens::contains)
                .count();
        int maxSize = Math.max((int) leftTokens.stream().distinct().count(), (int) rightTokens.stream().distinct().count());
        return maxSize == 0 ? 0d : ((double) intersection / maxSize);
    }

    private String buildBodySectionCopyMarkup(Map<String, Object> section, List<Map<String, Object>> bodySectionsCopy) {
        String sectionId = section == null ? null : asTrimmedString(section.get("sectionId"));
        if (!StringUtils.hasText(sectionId) || bodySectionsCopy == null || bodySectionsCopy.isEmpty()) {
            return "";
        }

        List<String> orderedSlotIds = extractCopySlots(section);
        if (!orderedSlotIds.isEmpty()) {
            StringBuilder slotted = new StringBuilder();
            for (String slotId : orderedSlotIds) {
                for (Map<String, Object> bodySection : bodySectionsCopy) {
                    if (!slotId.equalsIgnoreCase(asTrimmedString(bodySection.get("slotId")))) {
                        continue;
                    }
                    appendBodySectionBlock(slotted, section, bodySection);
                }
            }
            if (slotted.length() > 0) {
                return slotted.toString();
            }
        }
        for (Map<String, Object> bodySection : bodySectionsCopy) {
            String bodySectionId = asTrimmedString(bodySection.get("sectionId"));
            if (!StringUtils.hasText(bodySectionId) || !sectionId.equalsIgnoreCase(bodySectionId)) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            appendBodySectionBlock(sb, section, bodySection);
            return sb.toString();
        }
        return "";
    }



    @SuppressWarnings("unchecked")
    private List<String> extractCopySlots(Map<String, Object> section) {
        if (section == null) {
            return List.of();
        }
        if (section.get("slotDefs") instanceof List<?> rawSlotDefs) {
            List<String> slotKeys = rawSlotDefs.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> asTrimmedString(((Map<String, Object>) item).get("slotKey")))
                    .filter(StringUtils::hasText)
                    .toList();
            if (!slotKeys.isEmpty()) {
                return slotKeys;
            }
        }
        if (!(section.get("copySlots") instanceof List<?> rawSlots)) {
            return List.of();
        }
        return rawSlots.stream()
                .map(this::asTrimmedString)
                .filter(StringUtils::hasText)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void appendBodySectionBlock(StringBuilder sb, Map<String, Object> section, Map<String, Object> bodySection) {
        if (bodySection.get("items") instanceof List<?> rawItems) {
            Map<String, String> tagsById = buildTagsById(section);
            StringBuilder pendingListItems = new StringBuilder();
            for (Object rawItem : rawItems) {
                if (!(rawItem instanceof Map<?, ?> itemMap)) {
                    continue;
                }
                String itemId = asTrimmedString(((Map<String, Object>) itemMap).get("id"));
                String text = asTrimmedString(((Map<String, Object>) itemMap).get("texto"));
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                String tag = firstNonBlank(tagsById.get(itemId), "p");
                if ("li".equalsIgnoreCase(tag)) {
                    pendingListItems.append("<li>").append(escapeHtml(text)).append("</li>");
                    continue;
                }
                if (pendingListItems.length() > 0) {
                    sb.append("<ul>").append(pendingListItems).append("</ul>");
                    pendingListItems.setLength(0);
                }
                if ("h1".equalsIgnoreCase(tag) || "h2".equalsIgnoreCase(tag) || "h3".equalsIgnoreCase(tag)) {
                    sb.append("<").append(tag.toLowerCase(Locale.ROOT)).append(">")
                            .append(escapeHtml(text))
                            .append("</").append(tag.toLowerCase(Locale.ROOT)).append(">");
                } else {
                    appendParagraph(sb, text);
                }
            }
            if (pendingListItems.length() > 0) {
                sb.append("<ul>").append(pendingListItems).append("</ul>");
            }
            return;
        }
        appendRichText(sb, asTrimmedString(bodySection.get("summary")));
        appendRichText(sb, asTrimmedString(bodySection.get("copy")));
        if (bodySection.get("bullets") instanceof List<?> rawBullets) {
            StringBuilder bulletsMarkup = new StringBuilder();
            for (Object rawBullet : rawBullets) {
                String bullet = asTrimmedString(rawBullet);
                if (!StringUtils.hasText(bullet)) {
                    continue;
                }
                bulletsMarkup.append("<li>").append(escapeHtml(bullet)).append("</li>");
            }
            if (bulletsMarkup.length() > 0) {
                sb.append("<ul>").append(bulletsMarkup).append("</ul>");
            }
        }
        appendParagraph(sb, asTrimmedString(bodySection.get("ctaSupport")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> buildTagsById(Map<String, Object> section) {
        if (section == null || !(section.get("elementosSeccao") instanceof List<?> rawElements)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, String> tagsById = new java.util.LinkedHashMap<>();
        for (Object rawElement : rawElements) {
            collectTagsById(rawElement, tagsById);
        }
        return tagsById;
    }

    @SuppressWarnings("unchecked")
    private void collectTagsById(Object node, Map<String, String> target) {
        if (!(node instanceof Map<?, ?> rawMap)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) rawMap;
        String id = asTrimmedString(map.get("id"));
        String tag = asTrimmedString(map.get("tag"));
        if (StringUtils.hasText(id) && StringUtils.hasText(tag)) {
            target.put(id, tag);
        }
        if (map.get("elementosInternos") instanceof List<?> internals) {
            for (Object child : internals) {
                collectTagsById(child, target);
            }
        }
    }
    private String buildFaqMarkup(String sectionId, List<Map<String, Object>> faqCopy) {
        if (!StringUtils.hasText(sectionId) || faqCopy == null || faqCopy.isEmpty()) {
            return "";
        }
        if (!sectionId.toLowerCase().contains("faq")) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"faq-list\">");
        for (Map<String, Object> faqItem : faqCopy) {
            String question = asTrimmedString(faqItem.get("question"));
            String answer = asTrimmedString(faqItem.get("answer"));
            if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
                continue;
            }
            sb.append("<details><summary>")
                    .append(escapeHtml(question))
                    .append("</summary><p>")
                    .append(escapeHtml(answer))
                    .append("</p></details>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildCtaBlocksMarkup(String sectionId, List<Map<String, Object>> ctaBlocksCopy) {
        if (!StringUtils.hasText(sectionId) || ctaBlocksCopy == null || ctaBlocksCopy.isEmpty()) {
            return "";
        }
        String lowerSectionId = sectionId.toLowerCase();
        String placementHint = lowerSectionId.contains("sticky") ? "sticky"
                : lowerSectionId.contains("final") || lowerSectionId.contains("cta") ? "final"
                : lowerSectionId.contains("hero") ? "hero"
                : "mid";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> ctaBlock : ctaBlocksCopy) {
            String placement = asTrimmedString(ctaBlock.get("placement"));
            String ctaVariant = asTrimmedString(ctaBlock.get("ctaVariant"));
            if (StringUtils.hasText(placement) && !placementHint.equalsIgnoreCase(placement)) {
                continue;
            }
            if (StringUtils.hasText(ctaVariant) && !placementHint.equalsIgnoreCase(ctaVariant) && !lowerSectionId.contains("cta")) {
                continue;
            }
            String ctaLabel = asTrimmedString(ctaBlock.get("ctaLabel"));
            String ctaUrl = asTrimmedString(ctaBlock.get("ctaUrl"));
            String ctaSupport = asTrimmedString(ctaBlock.get("ctaSupport"));
            if (StringUtils.hasText(ctaLabel) && StringUtils.hasText(ctaUrl)) {
                sb.append("<p><a class=\"hero-cta-link\" href=\"")
                        .append(escapeAttr(ctaUrl))
                        .append("\" target=\"_blank\" rel=\"noopener noreferrer\">")
                        .append(escapeHtml(ctaLabel))
                        .append("</a></p>");
            }
            appendRichText(sb, ctaSupport);
        }
        return sb.toString();
    }

    private void appendParagraph(StringBuilder sb, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        appendRichText(sb, text);
    }


    private void appendRichText(StringBuilder sb, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String normalized = text.replace("\r\n", "\n").trim();
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        String[] blocks = normalized.split("\n\s*\n");
        for (String rawBlock : blocks) {
            String block = rawBlock.trim();
            if (!StringUtils.hasText(block)) {
                continue;
            }
            if (block.lines().allMatch(line -> line.trim().matches("[-*]\\s+.+"))) {
                sb.append("<ul>");
                block.lines().forEach(line -> sb.append("<li>").append(formatInlineText(line.replaceFirst("^[-*]\\s+", ""))).append("</li>"));
                sb.append("</ul>");
                continue;
            }
            if (block.lines().allMatch(line -> line.trim().matches("\\d+[).]\\s+.+"))) {
                sb.append("<ol>");
                block.lines().forEach(line -> sb.append("<li>").append(formatInlineText(line.replaceFirst("^\\d+[).]\\s+", ""))).append("</li>"));
                sb.append("</ol>");
                continue;
            }
            String paragraph = block.replace("\n", "<br>");
            sb.append("<p>").append(formatInlineText(paragraph)).append("</p>");
        }
    }

    private String formatInlineText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String escaped = escapeHtml(value);
        return escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
    }
    private String buildSubmissionScript(String formId) {
        return """
                <script>
                (function(){
                  const form = document.getElementById('%s');
                  if (!form) return;
                  const submitButton = form.querySelector('button[type="submit"]');
                  const feedback = document.getElementById('form-feedback');
                  form.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    if (!form.checkValidity()) {
                      form.reportValidity();
                      return;
                    }
                    const originalLabel = submitButton ? submitButton.textContent : '';
                    try {
                      if (submitButton) {
                        submitButton.disabled = true;
                        submitButton.textContent = 'Enviando...';
                      }
                      const response = await fetch(form.action, { method: form.method.toUpperCase(), body: new FormData(form) });
                      if (!response.ok) {
                        throw new Error('Falha ao enviar formulário');
                      }
                      if (feedback) {
                        feedback.style.display = 'block';
                        feedback.dataset.state = 'success';
                        feedback.textContent = 'Recebemos seu envio. Verifique seu e-mail.';
                      }
                    } catch (error) {
                      if (feedback) {
                        feedback.style.display = 'block';
                        feedback.dataset.state = 'error';
                        feedback.textContent = 'Não foi possível enviar agora. Tente novamente.';
                      }
                    } finally {
                      if (submitButton) {
                        submitButton.disabled = false;
                        submitButton.textContent = originalLabel || 'Enviar';
                      }
                    }
                  });
                })();
                </script>
                """.formatted(escapeAttr(formId));
    }

    private boolean containsFormBlock(String sectionId, Map<String, Object> section) {
        String contentType = asTrimmedString(section.get("contentType"));
        return "form".equalsIgnoreCase(contentType) || "form".equalsIgnoreCase(sectionId);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String escapeAttr(String value) {
        if (value == null) return "";
        return escapeHtml(value).replace("\"", "&quot;");
    }

    private String escapeCss(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", "").replace("{", "").replace("}", "");
    }

    private String normalizeCssToken(String value) {
        if (!StringUtils.hasText(value)) {
            return "normal";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')
                .replaceAll("[^a-z0-9-]", "");
        return StringUtils.hasText(normalized) ? normalized : "normal";
    }

    private record SectionVisualPreset(String sectionId, String surfaceStyle, String contrastMode) {
    }
}
