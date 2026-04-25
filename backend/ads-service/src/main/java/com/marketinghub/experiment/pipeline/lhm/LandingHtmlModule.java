package com.marketinghub.experiment.pipeline.lhm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Landing HTML Module (LHM): consolidates canonical inputs for the LANDING_PAGE_HTML prompt.
 */
@Component
public class LandingHtmlModule {

    private final ObjectMapper objectMapper;

    public LandingHtmlModule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String buildPromptV2Inputs(Experiment experiment) {
        if (experiment == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nPrompt v2 (inputs mínimos para LANDING_PAGE_HTML):\n");
        sb.append("Use apenas os 4 insumos abaixo como fonte de verdade para montar o HTML final.\n");
        appendIfPresent(sb, "1) Wireframe aprovado (JSON)", experiment.getLandingPageWireframe());
        appendIfPresent(sb, "2) Texto da landing aprovado (JSON)", experiment.getLandingPageCopy());
        appendIfPresent(sb, "3) Preset de design aprovado (JSON)", experiment.getLandingPageDesignPreset());
        String imageUrls = summarizeImageUrlsFromPlanning(experiment.getLandingPageImagePlanning());
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
        Map<String, Object> wireframeRoot = safeReadObject(experiment.getLandingPageWireframe());
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

        Map<String, Object> copyRoot = safeReadObject(experiment.getLandingPageCopy());
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

        List<Map<String, Object>> plannedImages = extractImages(experiment.getLandingPageImagePlanning());
        Map<String, Object> designRoot = safeReadObject(experiment.getLandingPageDesignPreset());
        Map<String, Object> designPreset = unwrapSectionPayload(designRoot, "landingPageDesignPreset");
        Map<String, Object> palette = extractPalette(designPreset);
        Map<String, Map<String, Object>> sectionDesignPresets = indexSectionDesignPresets(designPreset);

        String formId = firstNonBlank(asTrimmedString(formSpec.get("formId")), "lead-capture-primary");
        String submitTarget = firstNonBlank(asTrimmedString(formSpec.get("submitTarget")), "/api/flows/submissions");
        String submitLabel = firstNonBlank(asTrimmedString(formSpec.get("submitLabel")), "Enviar");

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"pt-BR\"><head>")
                .append("<meta charset=\"UTF-8\" />")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\" />")
                .append("<title>").append(escapeHtml(pageTitle)).append("</title>")
                .append("<style>")
                .append(":root{--bg:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("background")), "#f3f5f9")))
                .append(";--card:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("surface")), "#ffffff")))
                .append(";--border:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("border")), "#e3e8f2")))
                .append(";--text:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("textPrimary")), "#0f172a")))
                .append(";--muted:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("textMuted")), "#475569")))
                .append(";--brand:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("brandPrimary")), "#1d4ed8")))
                .append(";--brand-dark:").append(escapeCss(firstNonBlank(asTrimmedString(palette.get("brandSecondary")), "#1e40af")))
                .append(";}")
                .append("*{box-sizing:border-box;}body{font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;margin:0;background:linear-gradient(180deg,#f8fafc 0%,var(--bg) 100%);color:var(--text);line-height:1.55;}")
                .append("main{max-width:980px;margin:0 auto;padding:28px 16px 64px;display:grid;gap:14px;}")
                .append(".card{background:var(--card);border:1px solid var(--border);border-radius:18px;padding:18px;box-shadow:0 10px 30px rgba(15,23,42,.04);}")
                .append(".card[data-surface-contrast='high']{border-color:#cdd8ee;background:linear-gradient(180deg,#fff 0%,#f8fbff 100%);}")
                .append(".surface-band{background:linear-gradient(180deg,#ffffff 0%,#f8fbff 100%);}")
                .append(".surface-solid{background:#ffffff;}")
                .append(".surface-gradient-soft{background:linear-gradient(135deg,#f8fbff 0%,#eef4ff 100%);}")
                .append(".surface-image-tint{background:linear-gradient(135deg,#f8fafc 0%,#eef2ff 100%);}")
                .append(".contrast-high{border-color:#bfd0ee;box-shadow:0 12px 32px rgba(29,78,216,.08);}")
                .append(".contrast-soft{border-color:#e8edf6;background:#fbfdff;}")
                .append("h1{font-size:clamp(1.65rem,4vw,2.1rem);line-height:1.15;margin:0 0 10px;font-weight:800;letter-spacing:-0.02em;}")
                .append("h2{font-size:clamp(1.25rem,3.2vw,1.55rem);line-height:1.25;margin:0 0 10px;font-weight:750;letter-spacing:-0.01em;}")
                .append("p{margin:0 0 10px;color:var(--muted);font-size:.98rem;}")
                .append(".section-objective{font-size:.95rem;color:#334155;background:#f8fafc;border:1px dashed #d6e1f5;border-radius:12px;padding:10px 12px;margin:0 0 12px;}")
                .append("form{display:grid;gap:12px;background:#fbfdff;border:1px solid #dbe5f5;border-radius:14px;padding:14px;}")
                .append(".field{display:grid;gap:6px;}.help{color:#64748b;font-size:.88rem;line-height:1.35;}")
                .append("label{font-weight:700;font-size:.92rem;color:#0f172a;}input{width:100%;padding:11px 12px;border:1px solid #cfd8e6;border-radius:10px;background:#fff;font-size:1rem;outline:none;}")
                .append("input:focus{border-color:#3b82f6;box-shadow:0 0 0 3px rgba(59,130,246,.15);}button{padding:12px 16px;border:0;border-radius:999px;background:linear-gradient(135deg,var(--brand) 0%,var(--brand-dark) 100%);color:#fff;font-weight:800;cursor:pointer;letter-spacing:.01em;}")
                .append(".hero-cta-link{display:inline-block;padding:10px 14px;border-radius:999px;background:linear-gradient(135deg,var(--brand) 0%,var(--brand-dark) 100%);color:#fff;text-decoration:none;font-weight:800;}")
                .append("ul{margin:0 0 12px 18px;padding:0;}li{margin-bottom:6px;color:#1e293b;}")
                .append(".faq-list details{border:1px solid #dbe5f5;border-radius:12px;background:#fff;padding:10px 12px;margin:0 0 10px;}")
                .append(".faq-list summary{cursor:pointer;font-weight:700;color:#0f172a;}")
                .append("button:hover{filter:brightness(1.04);}img{max-width:100%;height:auto;border-radius:12px;display:block;margin-top:8px;}")
                .append("#form-feedback{display:none;margin-top:2px;font-weight:700;color:#1e3a8a;}")
                .append("</style></head><body><main>");

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
            String surfaceToken = firstNonBlank(asTrimmedString(surface.get("surfaceToken")), "surface-base");
            String surfaceStyle = firstNonBlank(asTrimmedString(surface.get("style")), "band");
            String surfaceContrast = firstNonBlank(asTrimmedString(surface.get("contrastMode")), "normal");
            Map<String, Object> sectionPreset = sectionDesignPresets.getOrDefault(sectionId.toLowerCase(), Map.of());
            surfaceStyle = firstNonBlank(asTrimmedString(sectionPreset.get("surfaceStyle")), surfaceStyle);
            surfaceContrast = firstNonBlank(asTrimmedString(sectionPreset.get("contrastMode")), surfaceContrast);
            String surfaceStyleClass = "surface-" + normalizeCssToken(surfaceStyle);
            String surfaceContrastClass = "contrast-" + normalizeCssToken(surfaceContrast);

            html.append("<section class=\"card ")
                    .append(escapeAttr(surfaceStyleClass))
                    .append(" ")
                    .append(escapeAttr(surfaceContrastClass))
                    .append("\" data-section-id=\"")
                    .append(escapeAttr(sectionId))
                    .append("\" data-surface-token=\"").append(escapeAttr(surfaceToken))
                    .append("\" data-surface-style=\"").append(escapeAttr(surfaceStyle))
                    .append("\" data-surface-contrast=\"").append(escapeAttr(surfaceContrast))
                    .append("\">")
                    .append(sectionIndex == 0
                            ? "<h1>" + escapeHtml(pageTitle) + "</h1>"
                            : "<h2>" + escapeHtml(sectionName) + "</h2>");
            if (sectionIndex == 0 && StringUtils.hasText(pageSummary)) {
                html.append("<p>").append(escapeHtml(pageSummary)).append("</p>");
            }
            if (sectionIndex == 0) {
                html.append(buildHeroCopyMarkup(heroCopy, submitLabel, submitTarget));
            }
            if (sectionIndex > 0 && StringUtils.hasText(sectionObjective)) {
                html.append("<p class=\"section-objective\">").append(escapeHtml(sectionObjective)).append("</p>");
            }
            html.append(buildBodySectionCopyMarkup(sectionId, bodySectionsCopy));
            html.append(buildFaqMarkup(sectionId, faqCopy));
            html.append(buildCtaBlocksMarkup(sectionId, ctaBlocksCopy));

            for (Map<String, Object> image : plannedImages) {
                String imageSectionId = asTrimmedString(image.get("sectionId"));
                if (!sectionId.equalsIgnoreCase(firstNonBlank(imageSectionId, ""))) {
                    continue;
                }
                html.append(buildImageTag(image));
            }

            if (containsFormBlock(sectionId, section)) {
                html.append(buildFormMarkup(formId, submitTarget, submitLabel, formSpec));
            }
            html.append("</section>");
        }

        html.append("</main>").append(buildSubmissionScript(formId)).append("</body></html>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String summarizeImageUrlsFromPlanning(String imagePlanningPayload) {
        if (!StringUtils.hasText(imagePlanningPayload)) {
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(imagePlanningPayload, Map.class);
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
        } catch (Exception ex) {
            return null;
        }
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

    private Map<String, Object> safeReadObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractImages(String raw) {
        Map<String, Object> root = safeReadObject(raw);
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
    private Map<String, Map<String, Object>> indexSectionDesignPresets(Map<String, Object> designPreset) {
        if (designPreset == null || designPreset.isEmpty()) {
            return Map.of();
        }
        if (!(designPreset.get("sectionPresets") instanceof List<?> rawSectionPresets)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Map<String, Object>> indexed = new java.util.LinkedHashMap<>();
        for (Object rawPreset : rawSectionPresets) {
            if (!(rawPreset instanceof Map<?, ?> rawPresetMap)) {
                continue;
            }
            Map<String, Object> preset = (Map<String, Object>) rawPresetMap;
            String sectionId = asTrimmedString(preset.get("sectionId"));
            if (!StringUtils.hasText(sectionId)) {
                continue;
            }
            indexed.put(sectionId.toLowerCase(), preset);
        }
        return indexed;
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

    private String buildImageTag(Map<String, Object> image) {
        String sectionId = firstNonBlank(asTrimmedString(image.get("sectionId")), "section");
        String bindingKey = firstNonBlank(asTrimmedString(image.get("imageBindingKey")), "image");
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

    private String buildHeroCopyMarkup(Map<String, Object> heroCopy, String fallbackSubmitLabel, String fallbackSubmitTarget) {
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
        if (StringUtils.hasText(promise)) {
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

    private String buildBodySectionCopyMarkup(String sectionId, List<Map<String, Object>> bodySectionsCopy) {
        if (!StringUtils.hasText(sectionId) || bodySectionsCopy == null || bodySectionsCopy.isEmpty()) {
            return "";
        }
        for (Map<String, Object> bodySection : bodySectionsCopy) {
            String bodySectionId = asTrimmedString(bodySection.get("sectionId"));
            if (!StringUtils.hasText(bodySectionId) || !sectionId.equalsIgnoreCase(bodySectionId)) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            appendParagraph(sb, asTrimmedString(bodySection.get("summary")));
            appendParagraph(sb, asTrimmedString(bodySection.get("copy")));
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
            return sb.toString();
        }
        return "";
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
            appendParagraph(sb, ctaSupport);
        }
        return sb.toString();
    }

    private void appendParagraph(StringBuilder sb, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        sb.append("<p>").append(escapeHtml(text)).append("</p>");
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
        String normalized = value.trim().toLowerCase()
                .replace('_', '-')
                .replace(' ', '-')
                .replaceAll("[^a-z0-9-]", "");
        return StringUtils.hasText(normalized) ? normalized : "normal";
    }
}
