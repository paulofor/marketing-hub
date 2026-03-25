package com.marketinghub.worker.report;

import com.marketinghub.experiment.dto.ExperimentCampaignMetricDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.CreativeSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.CreativeVariantSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.ExperimentSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.HypothesisSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.InstantFormSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.LandingPageSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.LeadPortalFlowSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.LeadPortalQuestionSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.MarketNicheSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Responsável por transformar o snapshot de dados em um documento HTML auto contido.
 */
@Component
public class ExperimentReportRenderer {

    private static final DateTimeFormatter LOCAL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(new Locale("pt", "BR"));
    private static final String LANDING_PAGE_SCREENSHOT_TEMPLATE =
            "https://api.microlink.io/?url=%s&screenshot=true&meta=false&embed=screenshot.url";

    private final ExperimentReportProperties properties;

    public ExperimentReportRenderer(ExperimentReportProperties properties) {
        this.properties = properties;
    }

    public RenderedExperimentReport render(ExperimentReportRequestDetailDto request,
                                           ExperimentReportMaterialDto material) {
        Objects.requireNonNull(request, "request não pode ser nulo");
        Objects.requireNonNull(material, "material não pode ser nulo");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">")
                .append("<title>Relatório objetivo - Experimento ")
                .append(escape(material.getExperiment() != null ? material.getExperiment().getName() : ""))
                .append("</title>")
                .append("<style>")
                .append("body{font-family:'Inter',Arial,sans-serif;margin:0;padding:32px;background:#f6f7fb;color:#1e1e1e;}")
                .append("h1,h2,h3{margin:0 0 12px 0;}h1{font-size:28px;}h2{font-size:22px;border-bottom:2px solid #eceff4;padding-bottom:8px;margin-top:32px;}")
                .append(".card{background:#fff;border-radius:16px;padding:24px;margin-bottom:20px;box-shadow:0 10px 25px rgba(15,23,42,0.08);}")
                .append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px;}")
                .append(".badge{display:inline-block;padding:4px 10px;border-radius:24px;font-size:12px;background:#eef2ff;color:#312e81;font-weight:600;}")
                .append("table{width:100%;border-collapse:collapse;margin-top:12px;font-size:14px;}")
                .append("th,td{border-bottom:1px solid #e2e8f0;padding:10px;text-align:left;vertical-align:top;}")
                .append("th{background:#f8fafc;font-weight:600;color:#475569;}")
                .append(".muted{color:#6b7280;font-size:13px;}")
                .append(".tag{display:inline-block;background:#e0f2fe;color:#0369a1;font-size:11px;padding:3px 8px;border-radius:999px;margin-right:6px;margin-bottom:4px;}")
                .append(".media-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:18px;margin-top:16px;}")
                .append(".media-card{background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 12px 30px rgba(15,23,42,0.1);display:flex;flex-direction:column;}")
                .append(".media-card img{width:100%;height:220px;object-fit:cover;background:#f1f5f9;}")
                .append(".media-card__placeholder{height:220px;display:flex;align-items:center;justify-content:center;background:#f8fafc;color:#94a3b8;font-size:12px;font-weight:600;text-transform:uppercase;}")
                .append(".media-card__body{padding:18px;display:flex;flex-direction:column;gap:8px;}")
                .append(".media-card__title{font-weight:600;font-size:16px;}")
                .append(".media-card__copy{font-size:13px;color:#475569;line-height:1.4;}")
                .append(".media-card__meta{font-size:11px;color:#94a3b8;text-transform:uppercase;letter-spacing:0.08em;}")
                .append(".media-card__badge{align-self:flex-start;background:#e0f2fe;color:#0369a1;font-size:11px;padding:4px 10px;border-radius:999px;font-weight:600;}")
                .append("</style></head><body>");

        html.append("<div class='card'>");
        html.append("<h1>Relatório objetivo do experimento</h1>");
        html.append("<p class='muted'>Gerado automaticamente em ")
                .append(formatInstant(Instant.now()))
                .append(".</p>");
        appendExperimentOverview(html, request, material.getExperiment());
        html.append("</div>");

        appendNiche(html, material.getNiche());
        appendHypothesis(html, material.getHypothesis());
        appendCreatives(html, material.getCreatives());
        appendCreativeVariants(html, material.getCreativeVariants());
        appendLandingPages(html, material.getLandingPages());
        appendLeadPortalFlows(html, material.getLeadPortalFlows());
        appendInstantForm(html, material.getInstantForm());
        appendCampaignMetric(html, material.getCampaignMetric());
        appendFunnelStages(html, material.getFunnelStages());

        html.append("</body></html>");

        String filename = String.format("experiment-report-exp%s-req%d.html",
                request.getExperimentId() != null ? request.getExperimentId() : "na",
                request.getId());
        byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
        return new RenderedExperimentReport(filename, "text/html; charset=UTF-8", bytes);
    }

    private void appendExperimentOverview(StringBuilder html,
                                          ExperimentReportRequestDetailDto request,
                                          ExperimentSnapshot experiment) {
        html.append("<div class='grid'>");
        html.append(card("Experimento", experiment != null ? escape(experiment.getName()) : "Não informado"));
        html.append(card("Status", experiment != null ? badge(experiment.getStatus()) : badge("DESCONHECIDO")));
        html.append(card("Período", formatExperimentPeriod(experiment)));
        html.append(card("Budget diário", formatCurrency(experiment != null ? experiment.getDailyBudget() : null)));
        html.append(card("Solicitado em", formatInstant(request.getRequestedAt())));
        html.append(card("ID", String.format("Exp. %s / Req. %d",
                experiment != null ? experiment.getId() : "?",
                request.getId())));
        html.append("</div>");
    }

    private void appendNiche(StringBuilder html, MarketNicheSnapshot niche) {
        if (niche == null) {
            return;
        }
        html.append(sectionHeader("Contexto do nicho"));
        wrapCard(html, () -> {
            appendDefinition(html, "Nome", escape(niche.getName()));
            appendDefinition(html, "Descrição", escape(niche.getDescription()));
            appendDefinition(html, "Interesses", joinList(niche.getInterestList()));
            appendDefinition(html, "Cargos", joinList(niche.getRoleList()));
            appendDefinition(html, "Comportamentos", joinList(niche.getBehaviorList()));
        });
    }

    private void appendHypothesis(StringBuilder html, HypothesisSnapshot hypothesis) {
        if (hypothesis == null) {
            return;
        }
        html.append(sectionHeader("Hipótese do experimento"));
        wrapCard(html, () -> {
            appendDefinition(html, "Título", escape(hypothesis.getTitle()));
            appendDefinition(html, "Promessa", escape(hypothesis.getPromise()));
            appendDefinition(html, "Problema", escape(hypothesis.getProblem()));
            appendDefinition(html, "Persona", escape(hypothesis.getPersona()));
            appendDefinition(html, "Mecanismo único", escape(hypothesis.getUniqueMechanism()));
        });
    }

    private void appendCreatives(StringBuilder html, List<CreativeSnapshot> creatives) {
        if (CollectionUtils.isEmpty(creatives)) {
            return;
        }
        html.append(sectionHeader("Principais criativos"));
        html.append("<div class='card'><table><thead><tr><th>Headline</th><th>CTA</th><th>URL</th><th>Ângulos / Provas</th></tr></thead><tbody>");
        creatives.stream()
                .limit(Math.max(1, properties.getMaxCreatives()))
                .forEach(creative -> {
                    html.append("<tr><td>")
                            .append(emphasize(creative.getHeadline()))
                            .append("<div class='muted'>")
                            .append(escape(creative.getPrimaryText()))
                            .append("</div></td><td>")
                            .append(escape(creative.getCta()))
                            .append("</td><td>")
                            .append(linkOrPlaceholder(creative.getDestinationUrl()))
                            .append("</td><td>")
                            .append(renderTags(creative.getAngles()))
                            .append(renderTags(creative.getVisualProofs()))
                            .append("</td></tr>");
                });
        html.append("</tbody></table></div>");
        appendCreativeGallery(html, creatives);
    }

    private void appendCreativeVariants(StringBuilder html, List<CreativeVariantSnapshot> variants) {
        if (CollectionUtils.isEmpty(variants)) {
            return;
        }
        html.append(sectionHeader("Variações criativas"));
        wrapCard(html, () -> variants.forEach(variant -> {
            html.append("<div style='margin-bottom:16px;'>")
                    .append("<strong>").append(escape(variant.getType())).append("</strong>")
                    .append("<div class='muted'>").append(linkOrPlaceholder(variant.getAssetUrl())).append("</div>")
                    .append("<div>").append(renderTags(variant.getTitles())).append("</div>")
                    .append("</div>");
        }));
    }

    private void appendLandingPages(StringBuilder html, List<LandingPageSnapshot> pages) {
        if (CollectionUtils.isEmpty(pages)) {
            return;
        }
        html.append(sectionHeader("Landing pages"));
        html.append("<div class='card'><table><thead><tr><th>URL</th><th>Tipo</th><th>Status</th></tr></thead><tbody>");
        pages.stream()
                .sorted(Comparator.comparing(LandingPageSnapshot::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(page -> {
                    html.append("<tr><td>").append(linkOrPlaceholder(page.getUrl())).append("</td>")
                            .append("<td>").append(escape(page.getType())).append("</td>")
                            .append("<td>").append(escape(page.getStatus())).append("</td></tr>");
                });
        html.append("</tbody></table></div>");
        appendLandingPagePreviews(html, pages);
    }

    private void appendLeadPortalFlows(StringBuilder html, List<LeadPortalFlowSnapshot> flows) {
        if (CollectionUtils.isEmpty(flows)) {
            return;
        }
        html.append(sectionHeader("Formulários do Lead Portal"));
        html.append("<div class='card'><div class='media-grid'>");
        flows.forEach(flow -> {
            html.append("<div class='media-card'>");
            String flowName = StringUtils.hasText(flow.getName()) ? flow.getName() : "Formulário Lead Portal";
            appendMediaPreviewImage(html, flow.getPreviewImageUrl(),
                    "Prévia do formulário " + flowName, "Sem imagem do formulário");
            html.append("<div class='media-card__body'>")
                    .append("<div class='media-card__meta'>Formulário</div>")
                    .append("<div class='media-card__title'>").append(escape(flowName)).append("</div>")
                    .append("<div class='media-card__copy'>Modelo: ")
                    .append(escape(flow.getModel()))
                    .append(flow.isApproved() ? " • aprovado" : " • pendente")
                    .append("</div>")
                    .append("<div>").append(renderTags(firstQuestions(flow.getQuestions()))).append("</div>");
            if (StringUtils.hasText(flow.getSlug())) {
                html.append("<div class='media-card__copy'>Slug: ")
                        .append(escape(flow.getSlug()))
                        .append("</div>");
            }
            html.append("</div></div>");
        });
        html.append("</div></div>");
    }

    private void appendCreativeGallery(StringBuilder html, List<CreativeSnapshot> creatives) {
        List<CreativeSnapshot> withImage = creatives.stream()
                .filter(creative -> StringUtils.hasText(creative.getImageUrl()))
                .limit(Math.max(1, properties.getMaxCreatives()))
                .collect(Collectors.toList());
        if (withImage.isEmpty()) {
            return;
        }
        html.append("<div class='card'><div class='media-grid'>");
        withImage.forEach(creative -> {
            html.append("<div class='media-card'>");
            String headline = StringUtils.hasText(creative.getHeadline()) ? creative.getHeadline() : "Criativo";
            appendMediaPreviewImage(html, creative.getImageUrl(),
                    "Prévia do anúncio " + headline, "Sem imagem do anúncio");
            html.append("<div class='media-card__body'>")
                    .append("<div class='media-card__meta'>Anúncio</div>")
                    .append("<div class='media-card__title'>").append(escape(headline)).append("</div>");
            if (StringUtils.hasText(creative.getPrimaryText())) {
                html.append("<div class='media-card__copy'>")
                        .append(escape(creative.getPrimaryText()))
                        .append("</div>");
            }
            if (StringUtils.hasText(creative.getCta())) {
                html.append("<span class='media-card__badge'>")
                        .append(escape(creative.getCta()))
                        .append("</span>");
            }
            if (StringUtils.hasText(creative.getDestinationUrl())) {
                html.append("<div class='media-card__copy'>Destino: ")
                        .append(linkOrPlaceholder(creative.getDestinationUrl()))
                        .append("</div>");
            }
            html.append("</div></div>");
        });
        html.append("</div></div>");
    }

    private void appendLandingPagePreviews(StringBuilder html, List<LandingPageSnapshot> pages) {
        List<LandingPageSnapshot> withUrl = pages.stream()
                .filter(page -> StringUtils.hasText(page.getUrl()))
                .limit(Math.max(1, properties.getMaxCreatives()))
                .collect(Collectors.toList());
        if (withUrl.isEmpty()) {
            return;
        }
        html.append("<div class='card'><div class='media-grid'>");
        withUrl.forEach(page -> {
            html.append("<div class='media-card'>");
            appendMediaPreviewImage(html, buildLandingPageScreenshot(page.getUrl()),
                    "Prévia da landing page", "Sem imagem da landing page");
            html.append("<div class='media-card__body'>")
                    .append("<div class='media-card__meta'>Landing page</div>")
                    .append("<div class='media-card__title'>").append(escape(landingPageTitle(page))).append("</div>")
                    .append("<div class='media-card__copy'>Tipo: ")
                    .append(escape(page.getType()))
                    .append(" • Status: ")
                    .append(escape(page.getStatus()))
                    .append("</div>")
                    .append("<div class='media-card__copy'>Destino: ")
                    .append(linkOrPlaceholder(page.getUrl()))
                    .append("</div>");
            html.append("</div></div>");
        });
        html.append("</div></div>");
    }

    private void appendMediaPreviewImage(StringBuilder html, String url, String altText, String placeholder) {
        if (StringUtils.hasText(url)) {
            html.append("<img src='")
                    .append(escape(url))
                    .append("' alt='")
                    .append(escape(altText))
                    .append("' loading='lazy'>");
        } else {
            html.append("<div class='media-card__placeholder'>")
                    .append(escape(placeholder))
                    .append("</div>");
        }
    }

    private String landingPageTitle(LandingPageSnapshot page) {
        if (page == null) {
            return "Landing page";
        }
        if (StringUtils.hasText(page.getType())) {
            return page.getType();
        }
        if (StringUtils.hasText(page.getUrl())) {
            try {
                URI uri = URI.create(page.getUrl());
                if (StringUtils.hasText(uri.getHost())) {
                    return uri.getHost();
                }
            } catch (IllegalArgumentException ignored) {
                return page.getUrl();
            }
            return page.getUrl();
        }
        return "Landing page";
    }

    private String buildLandingPageScreenshot(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8);
        return String.format(LANDING_PAGE_SCREENSHOT_TEMPLATE, encoded);
    }

    private List<String> firstQuestions(List<LeadPortalQuestionSnapshot> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return List.of();
        }
        return questions.stream()
                .map(LeadPortalQuestionSnapshot::getTitle)
                .filter(StringUtils::hasText)
                .limit(5)
                .collect(Collectors.toList());
    }

    private void appendInstantForm(StringBuilder html, InstantFormSnapshot instantForm) {
        if (instantForm == null) {
            return;
        }
        html.append(sectionHeader("Instant Form"));
        wrapCard(html, () -> {
            appendDefinition(html, "Nome", escape(instantForm.getName()));
            appendDefinition(html, "Status", escape(instantForm.getStatus()));
            appendDefinition(html, "Link público", linkOrPlaceholder(instantForm.getShareLink()));
        });
    }

    private void appendCampaignMetric(StringBuilder html, ExperimentCampaignMetricDto metric) {
        if (metric == null) {
            return;
        }
        html.append(sectionHeader("Métricas da campanha"));
        html.append("<div class='card'><div class='grid'>");
        appendMetric(html, "Impressões", formatNumber(metric.getImpressions()));
        appendMetric(html, "Cliques", formatNumber(metric.getClicks()));
        appendMetric(html, "Leads", formatNumber(metric.getLeads()));
        appendMetric(html, "Investimento", formatCurrency(metric.getSpend()));
        appendMetric(html, "CPC", formatCurrency(metric.getCpc()));
        appendMetric(html, "CPL", formatCurrency(metric.getCpl()));
        html.append("</div></div>");
    }

    private void appendFunnelStages(StringBuilder html, List<ExperimentFunnelStageDto> stages) {
        if (CollectionUtils.isEmpty(stages)) {
            return;
        }
        html.append(sectionHeader("Funil do experimento"));
        html.append("<div class='card'><table><thead><tr><th>Etapa</th><th>Total</th><th>Último evento</th></tr></thead><tbody>");
        stages.forEach(stage -> html.append("<tr><td>")
                .append(escape(stage.getLabel() != null ? stage.getLabel() : String.valueOf(stage.getStage())))
                .append("</td><td>")
                .append(formatNumber(stage.getTotalCount()))
                .append("</td><td>")
                .append(formatInstant(stage.getLastEventAt()))
                .append("</td></tr>"));
        html.append("</tbody></table></div>");
    }

    private String sectionHeader(String title) {
        return "<h2>" + escape(title) + "</h2>";
    }

    private String card(String title, String value) {
        return "<div><div class='muted'>" + escape(title) + "</div><div style='font-size:18px;font-weight:600;'>"
                + value + "</div></div>";
    }

    private String badge(String value) {
        return "<span class='badge'>" + escape(value) + "</span>";
    }

    private void appendDefinition(StringBuilder html, String label, String value) {
        html.append("<div style='margin-bottom:10px;'><div class='muted'>")
                .append(escape(label))
                .append("</div><div>")
                .append(value)
                .append("</div></div>");
    }

    private void wrapCard(StringBuilder html, Runnable content) {
        html.append("<div class='card'>");
        content.run();
        html.append("</div>");
    }

    private void appendMetric(StringBuilder html, String label, String value) {
        html.append("<div><div class='muted'>").append(escape(label)).append("</div><div style='font-size:20px;font-weight:600;'>")
                .append(value)
                .append("</div></div>");
    }

    private String renderTags(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return "<span class='muted'>Sem dados</span>";
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(this::escape)
                .map(val -> "<span class='tag'>" + val + "</span>")
                .collect(Collectors.joining());
    }

    private String formatExperimentPeriod(ExperimentSnapshot experiment) {
        if (experiment == null) {
            return "N/D";
        }
        String start = experiment.getStartDate() != null ? experiment.getStartDate().format(LOCAL_DATE_FORMATTER) : "?";
        String end = experiment.getEndDate() != null ? experiment.getEndDate().format(LOCAL_DATE_FORMATTER) : "–";
        return start + " a " + end;
    }

    private String emphasize(String value) {
        return "<strong>" + escape(value) + "</strong>";
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "N/D";
        }
        return DATE_TIME_FORMATTER.format(instant);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "N/D";
        }
        return CURRENCY_FORMAT.format(value);
    }

    private String formatNumber(Number number) {
        if (number == null) {
            return "N/D";
        }
        return NUMBER_FORMAT.format(number);
    }

    private String linkOrPlaceholder(String url) {
        if (!StringUtils.hasText(url)) {
            return "<span class='muted'>Não definido</span>";
        }
        String escaped = escape(url);
        return "<a href='" + escaped + "' target='_blank' rel='noreferrer'>" + escaped + "</a>";
    }

    private String joinList(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return "<span class='muted'>Sem registros</span>";
        }
        return escape(values.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", ")));
    }

    private String escape(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return HtmlUtils.htmlEscape(value);
    }

    public record RenderedExperimentReport(String filename, String contentType, byte[] content) {}
}
