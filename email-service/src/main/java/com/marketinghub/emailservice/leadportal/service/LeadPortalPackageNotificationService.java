package com.marketinghub.emailservice.leadportal.service;

import com.marketinghub.emailservice.service.client.FlowSubmissionImagePackageStatus;
import com.marketinghub.emailservice.leadportal.integration.LeadPortalPaymentsClient;
import com.marketinghub.emailservice.leadportal.integration.LeadPortalPaymentsClient.PaymentCheckoutResponse;
import com.marketinghub.emailservice.storage.FileStorageService;
import com.marketinghub.emailservice.storage.StorageException;
import com.marketinghub.emailservice.leadportal.email.LeadPortalEmailTemplatePlaceholder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Scans completed lead portal image packages and dispatches e-mails with watermarked previews.
 */
@Service
public class LeadPortalPackageNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPackageNotificationService.class);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(new Locale("pt", "BR"))
            .withZone(ZoneId.systemDefault());

    // Copy fixa do produto
    private static final String PRODUCT_OWNER = "Produtividade 360";
    private static final BigDecimal DEFAULT_PRODUCT_PRICE = new BigDecimal("127.00");
    private static final String DEFAULT_PRODUCT_CURRENCY = "BRL";
    private static final String DEFAULT_PRODUCT_DESCRIPTION = "pacote com 10 imagens para posts em redes sociais";
    private static final String DEFAULT_EMAIL_PREHEADER = "Suas amostras com marca d'água estão prontas. Veja a prévia e libere o pacote completo com 10 imagens premium.";
    private static final int INLINE_PREVIEW_LIMIT = 3;
    private static final String[] INLINE_PREVIEW_CAPTIONS = {
            "Visual premium para destacar seu posicionamento.",
            "Artes pensadas para valorizar sua imagem nas redes.",
            "Uma prévia real da qualidade do pacote completo."
    };
    private static final PreviewCardStyle[] PREVIEW_CARD_STYLES = {
            new PreviewCardStyle("#f6f2ff", "#eadffd"),
            new PreviewCardStyle("#fff5ef", "#ffe1d1"),
            new PreviewCardStyle("#f3f6fb", "#dde5f3")
    };

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageService fileStorageService;
    private final LeadPortalImagePackageStatusHistoryService statusHistoryService;
    private final LeadPortalEmailTemplateService emailTemplateService;
    private final LeadPortalPaymentsClient paymentsClient;
    private final LeadPortalTrackingLinkService trackingLinkService;

    @Value("${lead-portal.notifications.max-attempts:5}")
    private int maxAttempts;

    @Value("${lead-portal.notifications.batch-size:3}")
    private int batchSize;

    @Value("${lead-portal.notifications.lock-seconds:300}")
    private int lockSeconds;

    public LeadPortalPackageNotificationService(
            JdbcTemplate jdbcTemplate,
            FileStorageService fileStorageService,
            LeadPortalImagePackageStatusHistoryService statusHistoryService,
            LeadPortalEmailTemplateService emailTemplateService,
            LeadPortalPaymentsClient paymentsClient,
            LeadPortalTrackingLinkService trackingLinkService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        this.statusHistoryService = statusHistoryService;
        this.emailTemplateService = emailTemplateService;
        this.paymentsClient = paymentsClient;
        this.trackingLinkService = trackingLinkService;
    }

    @PostConstruct
    void logConfiguration() {
        log.info("Lead portal package export ready (maxAttempts={}, batchSize={})", maxAttempts, batchSize);
    }

    public java.util.List<LeadPortalImagePackageExportItem> exportReadyPackages(int limit) {
        java.util.List<PendingPackage> packages = findPendingPackages(limit <= 0 ? batchSize : limit);
        java.util.List<LeadPortalImagePackageExportItem> exports = new java.util.ArrayList<>();
        for (PendingPackage pending : packages) {
            if (!lockPackage(pending.packageId())) {
                continue;
            }
            try {
                exports.add(buildExportItem(pending));
            } catch (Exception ex) {
                log.error("Failed to prepare export for lead portal package {}", pending.packageId(), ex);
                recordFailure(pending.packageId(), ex.getMessage());
            }
        }
        return exports;
    }

    private LeadPortalImagePackageExportItem buildExportItem(PendingPackage pending) throws IOException {
        if (!StringUtils.hasText(pending.zipObjectKey())) {
            throw new IllegalStateException("ZIP ainda não gerado para o pacote " + pending.packageId());
        }
        int imageCount = pending.imageCount() > 0 ? pending.imageCount() : countWatermarkedAssets(pending.packageId());
        byte[] zipBytes = loadObjectBytes(pending.zipObjectKey());
        PaymentInfo paymentInfo = ensurePaymentInfo(pending);
        List<InlinePreview> inlinePreviews = fetchInlinePreviews(pending.packageId());
        EmailContent content = buildEmailContent(pending, imageCount, paymentInfo, inlinePreviews);
        return new LeadPortalImagePackageExportItem(
                pending.packageId(),
                pending.submissionId(),
                pending.submissionName(),
                pending.submissionEmail(),
                pending.status(),
                pending.experimentId(),
                pending.experimentName(),
                pending.sampleSubject(),
                pending.samplePreview(),
                pending.sampleBody(),
                pending.sampleCallToAction(),
                pending.sampleModel(),
                pending.samplePrompt(),
                pending.sampleUpdatedAt(),
                pending.notificationAttempts(),
                pending.lastAttempt(),
                zipBytes,
                pending.zipObjectKey(),
                content.attachmentName(),
                imageCount,
                content.subject(),
                content.plain(),
                content.html(),
                new LeadPortalImagePackageExportItem.PaymentInfo(
                        paymentInfo.purchaseId(),
                        paymentInfo.checkoutUrl(),
                        paymentInfo.amount(),
                        paymentInfo.currency(),
                        paymentInfo.expiresAt(),
                        paymentInfo.statementDescriptor()));
    }

    private boolean lockPackage(long packageId) {
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notification_last_attempt = ?, notification_last_error = NULL WHERE id = ? "
                        + "AND (notification_last_attempt IS NULL OR notification_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))",
                Timestamp.from(Instant.now()),
                packageId,
                lockSeconds);
        return updated > 0;
    }

    public void acknowledgePackage(long packageId, boolean success, String errorMessage) {
        if (success) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacote de imagem não encontrado"));
            markAsNotified(packageId, current);
        } else {
            recordFailure(packageId, errorMessage != null ? errorMessage : "Falha desconhecida");
        }
    }

    private List<PendingPackage> findPendingPackages(int limit) {
        String sql = """
                SELECT
                    pack.id AS package_id,
                    pack.submission_id,
                    pack.status,
                    pack.notification_attempts,
                    pack.notification_last_attempt,
                    pack.zip_object_key,
                    pack.zip_size_bytes,
                    pack.zip_generated_at,
                    (
                        SELECT COUNT(*) FROM flow_submission_image_watermark wm
                          JOIN flow_submission_image_item items2 ON items2.id = wm.item_id
                        WHERE items2.package_id = pack.id
                    ) AS watermarked_count,
                    sub.name AS submission_name,
                    sub.email AS submission_email,
                    exp.id AS experiment_id,
                    COALESCE(exp.name, flow.name, flow.slug) AS experiment_name,
                    sample.subject AS sample_subject,
                    sample.preview_text AS sample_preview,
                    sample.body AS sample_body,
                    sample.call_to_action AS sample_call_to_action,
                    sample.model AS sample_model,
                    sample.prompt AS sample_prompt,
                    sample.updated_at AS sample_updated_at,
                    pack.payment_purchase_id,
                    pack.payment_checkout_url,
                    pack.payment_checkout_expires_at,
                    pack.payment_amount,
                    pack.payment_currency,
                    pack.payment_statement_descriptor
                FROM flow_submission_image_package pack
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                LEFT JOIN experiment_sample_email sample ON sample.id = exp.selected_sample_email_id
                WHERE sub.email IS NOT NULL AND sub.email <> ''
                  AND pack.notified_at IS NULL
                  AND pack.zip_object_key IS NOT NULL
                  AND pack.zip_generated_at IS NOT NULL
                  AND (pack.notification_last_attempt IS NULL
                       OR pack.notification_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                  AND pack.notification_attempts < ?
                  AND pack.status <> 'FAILED'
                  AND (
                        SELECT COUNT(*) FROM flow_submission_image_item items WHERE items.package_id = pack.id
                      ) > 0
                  AND (
                        SELECT COUNT(*) FROM flow_submission_image_watermark wm
                          JOIN flow_submission_image_item items2 ON items2.id = wm.item_id
                        WHERE items2.package_id = pack.id
                      ) >= (
                        SELECT COUNT(*) FROM flow_submission_image_item items WHERE items.package_id = pack.id
                      )
                ORDER BY pack.updated_at ASC, pack.id ASC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPendingPackage(rs), lockSeconds, maxAttempts, limit);
    }

    private PendingPackage mapPendingPackage(ResultSet rs) throws SQLException {
        return new PendingPackage(
                rs.getLong("package_id"),
                normalizeSubmissionId(rs.getObject("submission_id")),
                FlowSubmissionImagePackageStatus.valueOf(rs.getString("status")),
                rs.getInt("notification_attempts"),
                toInstant(rs.getTimestamp("notification_last_attempt")),
                rs.getString("submission_name"),
                rs.getString("submission_email"),
                (Long) rs.getObject("experiment_id"),
                rs.getString("experiment_name"),
                rs.getString("sample_subject"),
                rs.getString("sample_preview"),
                rs.getString("sample_body"),
                rs.getString("sample_call_to_action"),
                rs.getString("sample_model"),
                rs.getString("sample_prompt"),
                toInstant(rs.getTimestamp("sample_updated_at")),
                rs.getString("zip_object_key"),
                rs.getLong("zip_size_bytes"),
                toInstant(rs.getTimestamp("zip_generated_at")),
                rs.getInt("watermarked_count"),
                (Long) rs.getObject("payment_purchase_id"),
                rs.getString("payment_checkout_url"),
                toInstant(rs.getTimestamp("payment_checkout_expires_at")),
                rs.getBigDecimal("payment_amount"),
                rs.getString("payment_currency"),
                rs.getString("payment_statement_descriptor")
        );
    }

    private int countWatermarkedAssets(long packageId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flow_submission_image_watermark wm JOIN flow_submission_image_item item ON item.id = wm.item_id WHERE item.package_id = ?",
                Integer.class,
                packageId);
        return count != null ? count : 0;
    }

    private List<InlinePreview> fetchInlinePreviews(long packageId) {
        String sql = """
                SELECT
                    item.position_index,
                    wm_opt_asset.external_id AS watermark_optimized_external_id,
                    wm_opt_asset.url AS watermark_optimized_url,
                    wm_asset.external_id AS watermark_external_id,
                    wm_asset.url AS watermark_url
                FROM flow_submission_image_item item
                JOIN flow_submission_image_watermark wm ON wm.item_id = item.id
                LEFT JOIN asset wm_asset ON wm_asset.id = wm.asset_id
                LEFT JOIN asset wm_opt_asset ON wm_opt_asset.id = wm.optimized_asset_id
                WHERE item.package_id = ?
                ORDER BY item.position_index ASC, item.id ASC
                LIMIT ?
                """;
        List<InlinePreview> previews = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String storedFileName = firstNonBlank(
                    rs.getString("watermark_optimized_external_id"),
                    rs.getString("watermark_optimized_url"),
                    rs.getString("watermark_external_id"),
                    rs.getString("watermark_url"));
            String resolvedUrl = firstNonBlank(
                    fileStorageService.resolvePublicUrl(storedFileName).orElse(null),
                    rs.getString("watermark_optimized_url"),
                    rs.getString("watermark_url"));
            if (!StringUtils.hasText(resolvedUrl)) {
                return null;
            }
            return new InlinePreview(resolvedUrl.trim());
        }, packageId, INLINE_PREVIEW_LIMIT);
        return previews.stream().filter(Objects::nonNull).collect(Collectors.toList());
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

    private byte[] loadObjectBytes(String storedFileName) throws IOException {
        if (!StringUtils.hasText(storedFileName)) {
            throw new IOException("Nome do arquivo do bucket não informado");
        }
        try {
            try (InputStream in = fileStorageService.loadAsResource(storedFileName).getInputStream()) {
                return in.readAllBytes();
            }
        } catch (StorageException ex) {
            throw new IOException("Failed to load object '" + storedFileName + "'", ex);
        }
    }


    private EmailContent buildEmailContent(PendingPackage pending,
                                           int imageCount,
                                           PaymentInfo paymentInfo,
                                           List<InlinePreview> inlinePreviews) {
        TrackingLinks trackingLinks = buildTrackingLinks(pending);
        List<String> previewUrls = extractPreviewUrls(inlinePreviews);

        Optional<LeadPortalEmailTemplateService.LeadPortalEmailTemplate> template = emailTemplateService.findTemplate();
        String subject = resolveEmailSubject(pending, imageCount, template.map(LeadPortalEmailTemplateService.LeadPortalEmailTemplate::subject).orElse(null));
        if (template.isPresent() && StringUtils.hasText(template.get().html())) {
            EmailContent customContent = buildCustomEmailContent(
                    pending,
                    subject,
                    paymentInfo,
                    previewUrls,
                    template.get().html(),
                    trackingLinks);
            if (customContent != null) {
                return customContent;
            }
        }

        return buildDefaultEmailContent(
                pending,
                subject,
                imageCount,
                paymentInfo,
                inlinePreviews,
                trackingLinks);
    }

    private List<String> extractPreviewUrls(List<InlinePreview> inlinePreviews) {
        if (inlinePreviews == null || inlinePreviews.isEmpty()) {
            return List.of();
        }
        return inlinePreviews.stream()
                .map(InlinePreview::url)
                .filter(StringUtils::hasText)
                .limit(INLINE_PREVIEW_LIMIT)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private TrackingLinks buildTrackingLinks(PendingPackage pending) {
        String trackingViewUrl = trackingLinkService.buildPreviewUrl(pending.packageId(), pending.submissionId()).orElse(null);
        String trackingPixelUrl = trackingLinkService.buildPixelUrl(pending.packageId(), pending.submissionId()).orElse(null);
        return new TrackingLinks(trackingViewUrl, trackingPixelUrl);
    }

    private EmailContent buildCustomEmailContent(PendingPackage pending,
                                                 String subject,
                                                 PaymentInfo paymentInfo,
                                                 List<String> previewUrls,
                                                 String templateHtml,
                                                 TrackingLinks trackingLinks) {
        if (!StringUtils.hasText(templateHtml)) {
            return null;
        }
        Map<LeadPortalEmailTemplatePlaceholder, String> replacements =
                new EnumMap<>(LeadPortalEmailTemplatePlaceholder.class);
        replacements.put(LeadPortalEmailTemplatePlaceholder.LEAD_NAME, pending.submissionName());
        replacements.put(LeadPortalEmailTemplatePlaceholder.PAYMENT_LINK, paymentInfo != null ? paymentInfo.checkoutUrl() : null);
        replacements.put(LeadPortalEmailTemplatePlaceholder.PREVIEW_IMAGE_1, previewUrls.size() > 0 ? previewUrls.get(0) : null);
        replacements.put(LeadPortalEmailTemplatePlaceholder.PREVIEW_IMAGE_2, previewUrls.size() > 1 ? previewUrls.get(1) : null);
        replacements.put(LeadPortalEmailTemplatePlaceholder.PREVIEW_IMAGE_3, previewUrls.size() > 2 ? previewUrls.get(2) : null);

        String processedHtml = applyTemplateReplacements(templateHtml, replacements);
        if (!StringUtils.hasText(processedHtml)) {
            return null;
        }
        StringBuilder htmlBuilder = new StringBuilder(processedHtml.trim());
        appendTrackingMetadata(htmlBuilder, trackingLinks);
        String htmlContent = htmlBuilder.toString();
        String plainContent = htmlToPlainText(htmlContent);
        String attachmentName = "amostras-com-marca-dagua-" + pending.packageId() + ".zip";
        return new EmailContent(subject, plainContent, htmlContent, attachmentName);
    }

    private void appendTrackingMetadata(StringBuilder html, TrackingLinks trackingLinks) {
        if (trackingLinks != null && StringUtils.hasText(trackingLinks.pixelUrl())) {
            html.append("<img src=\"")
                    .append(HtmlUtils.htmlEscape(trackingLinks.pixelUrl()))
                    .append("\" alt=\"\" width=\"1\" height=\"1\" style=\"display:none;\" />");
        }
    }

    private String applyTemplateReplacements(String template,
                                             Map<LeadPortalEmailTemplatePlaceholder, String> replacements) {
        String result = template;
        for (LeadPortalEmailTemplatePlaceholder placeholder : LeadPortalEmailTemplatePlaceholder.values()) {
            String sanitized = sanitizeTemplateValue(replacements.get(placeholder));
            result = result.replace(placeholder.token(), sanitized);
        }
        return result;
    }

    private String sanitizeTemplateValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return HtmlUtils.htmlEscape(value.trim());
    }

    private String htmlToPlainText(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String normalized = html
                .replaceAll("(?i)<br\\s*/?>", "\\n")
                .replaceAll("(?i)</p>", "\\n\\n")
                .replaceAll("(?i)</div>", "\\n");
        String withoutTags = normalized.replaceAll("<[^>]+>", "");
        String unescaped = HtmlUtils.htmlUnescape(withoutTags);
        return unescaped.replaceAll("\\r", "")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private EmailContent buildDefaultEmailContent(PendingPackage pending,
                                                  String subject,
                                                  int imageCount,
                                                  PaymentInfo paymentInfo,
                                                  List<InlinePreview> inlinePreviews,
                                                  TrackingLinks trackingLinks) {
        String trackingViewUrl = trackingLinks != null ? trackingLinks.viewUrl() : null;
        String trackingPixelUrl = trackingLinks != null ? trackingLinks.pixelUrl() : null;
        String recipientName = StringUtils.hasText(pending.submissionName())
                ? pending.submissionName().trim()
                : null;

        BigDecimal resolvedAmount = paymentInfo != null && paymentInfo.amount() != null
                ? paymentInfo.amount()
                : DEFAULT_PRODUCT_PRICE;
        String resolvedCurrency = paymentInfo != null && StringUtils.hasText(paymentInfo.currency())
                ? paymentInfo.currency()
                : DEFAULT_PRODUCT_CURRENCY;
        String formattedPrice = formatCurrency(resolvedAmount, resolvedCurrency);
        String ctaLabel = StringUtils.hasText(formattedPrice)
                ? "Liberar pacote completo por " + formattedPrice
                : "Liberar pacote completo";

        String paymentUrl = paymentInfo != null ? paymentInfo.checkoutUrl() : null;
        String descriptor = paymentInfo != null && StringUtils.hasText(paymentInfo.statementDescriptor())
                ? paymentInfo.statementDescriptor()
                : "Mercado Pago";

        StringBuilder plain = new StringBuilder();
        plain.append(DEFAULT_EMAIL_PREHEADER).append("\n\n");
        plain.append("Olá");
        if (StringUtils.hasText(recipientName)) {
            plain.append(" ").append(recipientName);
        }
        plain.append(",\n\n");
        plain.append("Suas amostras estão prontas! Anexei ")
                .append(imageCount)
                .append(" imagem(ns) com marca d'água para você avaliar.\n");
        if (inlinePreviews != null && !inlinePreviews.isEmpty()) {
            plain.append("Algumas prévias rápidas:\n");
            for (int i = 0; i < inlinePreviews.size(); i++) {
                InlinePreview preview = inlinePreviews.get(i);
                plain.append("- Arte ")
                        .append(i + 1)
                        .append(": ")
                        .append(preview.url())
                        .append("\n");
            }
            plain.append("\n");
        }
        plain.append("Para liberar o pacote completo (")
                .append(DEFAULT_PRODUCT_DESCRIPTION)
                .append("), o valor é ")
                .append(formattedPrice != null ? formattedPrice : "R$ 127,00")
                .append(".\n");
        if (StringUtils.hasText(trackingViewUrl)) {
            plain.append("Ver prévias online: ")
                    .append(trackingViewUrl)
                    .append("\n");
        }

        appendPaymentCallToAction(plain, null, paymentInfo, "Liberar pacote completo");

        plain.append("Atenciosamente,\n")
                .append(PRODUCT_OWNER)
                .append("\n\n");
        plain.append("ID do pacote: ").append(pending.packageId());
        if (pending.sampleUpdatedAt() != null) {
            plain.append("\nE-mail atualizado em: ").append(HUMAN_DATE.format(pending.sampleUpdatedAt()));
        }

        String escapedName = StringUtils.hasText(recipientName) ? HtmlUtils.htmlEscape(recipientName) : null;
        String escapedExperiment = HtmlUtils.htmlEscape(resolveExperimentLabel(pending));
        String escapedPaymentUrl = StringUtils.hasText(paymentUrl) ? HtmlUtils.htmlEscape(paymentUrl) : null;
        String escapedFormattedPrice = StringUtils.hasText(formattedPrice)
                ? HtmlUtils.htmlEscape(formattedPrice)
                : null;
        String previewCards = buildInlinePreviewCards(inlinePreviews);
        String viewOnlineLink = StringUtils.hasText(trackingViewUrl)
                ? "Se não visualizar corretamente, <a href=\"" + HtmlUtils.htmlEscape(trackingViewUrl)
                + "\" target=\"_blank\" rel=\"noopener\" style=\"color:#7c8397;\">abra este e-mail no navegador</a>."
                : "Se não visualizar corretamente, abra este e-mail no navegador.";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"pt-BR\" xmlns=\"http://www.w3.org/1999/xhtml\">")
                .append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />")
                .append("<meta name=\"x-apple-disable-message-reformatting\" />")
                .append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />")
                .append("<title>Suas amostras estão prontas</title>")
                .append("<style>")
                .append("body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }")
                .append("table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }")
                .append("img { -ms-interpolation-mode: bicubic; }")
                .append("img { border: 0; height: auto; line-height: 100%; outline: none; text-decoration: none; display: block; }")
                .append("table { border-collapse: collapse !important; }")
                .append("body { margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; background-color: #eef1f8; }")
                .append(".preheader { display: none !important; visibility: hidden; opacity: 0; color: transparent; height: 0; width: 0; overflow: hidden; mso-hide: all; }")
                .append("@media screen and (max-width: 620px) { .wrapper { width: 100% !important; } .mobile-padding { padding-left: 20px !important; padding-right: 20px !important; } .mobile-stack, .mobile-stack td { display: block !important; width: 100% !important; } .mobile-center { text-align: center !important; } .fluid-img { width: 100% !important; max-width: 100% !important; } .button-cell a { display: block !important; } .spacer-mobile { height: 12px !important; } .headline { font-size: 28px !important; line-height: 34px !important; } .subheadline { font-size: 16px !important; line-height: 24px !important; } }")
                .append("</style></head>")
                .append("<body style=\"margin:0; padding:0; background-color:#eef1f8;\">")
                .append("<div class=\"preheader\">")
                .append(HtmlUtils.htmlEscape(DEFAULT_EMAIL_PREHEADER))
                .append("</div>")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\" style=\"background-color:#eef1f8;\">")
                .append("<tr><td align=\"center\" style=\"padding: 24px 12px;\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" class=\"wrapper\" role=\"presentation\" style=\"width:600px; max-width:600px;\">")
                .append("<tr><td align=\"center\" style=\"padding-bottom:16px; font-family:Arial, Helvetica, sans-serif; font-size:12px; line-height:18px; color:#7c8397;\">")
                .append(viewOnlineLink)
                .append("</td></tr>")
                .append("<tr><td style=\"background:linear-gradient(135deg, #171b31 0%, #4a2ea8 55%, #ff7a30 100%); border-radius:28px 28px 0 0; padding:28px 32px 20px 32px;\" class=\"mobile-padding\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\">")
                .append("<tr><td align=\"left\" class=\"mobile-center\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; color:#ffffff; font-weight:bold; letter-spacing:0.08em; text-transform:uppercase;\">")
                .append(HtmlUtils.htmlEscape(PRODUCT_OWNER))
                .append("</td></tr>")
                .append("<tr><td style=\"height:18px; line-height:18px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td class=\"headline mobile-center\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:34px; line-height:40px; color:#ffffff; font-weight:bold; letter-spacing:-0.02em;\">")
                .append("Suas amostras estão prontas")
                .append("</td></tr>")
                .append("<tr><td style=\"height:12px; line-height:12px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td class=\"subheadline mobile-center\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:18px; line-height:28px; color:#eef1ff;\">")
                .append("Olá")
                .append(escapedName != null ? ", <strong>" + escapedName + "</strong>. " : ", ")
                .append("Preparamos suas artes com marca d’água para mostrar como o seu perfil pode ficar mais profissional, organizado e valioso nas redes sociais.")
                .append("</td></tr>")
                .append("<tr><td style=\"height:22px; line-height:22px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td align=\"left\" class=\"mobile-center button-cell\">")
                .append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" role=\"presentation\" style=\"border-collapse:separate;\"><tr><td align=\"center\" bgcolor=\"#ff7a30\" style=\"border-radius:999px;\">");

        if (escapedPaymentUrl != null) {
            html.append("<a href=\"")
                    .append(escapedPaymentUrl)
                    .append("\" target=\"_blank\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:16px; line-height:16px; font-weight:bold; color:#ffffff; text-decoration:none; padding:16px 28px; display:inline-block; border-radius:999px;\">")
                    .append(HtmlUtils.htmlEscape(ctaLabel))
                    .append("</a>");
        } else {
            html.append("<span style=\"font-family:Arial, Helvetica, sans-serif; font-size:16px; line-height:16px; font-weight:bold; color:#ffffff; text-decoration:none; padding:16px 28px; display:inline-block; border-radius:999px;\">")
                    .append(HtmlUtils.htmlEscape(ctaLabel))
                    .append("</span>");
        }

        html.append("</td></tr></table>")
                .append("</td></tr>")
                .append("<tr><td style=\"height:12px; line-height:12px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td class=\"mobile-center\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px; line-height:20px; color:#f7ddcf;\">")
                .append("Pagamento simples e liberação após a confirmação.")
                .append("</td></tr>")
                .append("</table></td></tr>")

                .append("<tr><td style=\"background-color:#ffffff; padding:26px 32px 8px 32px;\" class=\"mobile-padding\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\">")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:22px; line-height:30px; color:#1f2433; font-weight:bold;\">")
                .append("Veja a prévia do seu material")
                .append("</td></tr>")
                .append("<tr><td style=\"height:10px; line-height:10px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:15px; line-height:24px; color:#5d667d;\">")
                .append("Anexamos <strong>")
                .append(imageCount)
                .append(" imagem(ns) com marca d’água</strong> para você avaliar o estilo, a qualidade e a personalização.")
                .append("</td></tr></table></td></tr>");

        if (previewCards != null && !previewCards.isBlank()) {
            html.append("<tr><td style=\"background-color:#ffffff; padding:10px 24px 8px 24px;\" class=\"mobile-padding\">")
                    .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\">")
                    .append(previewCards)
                    .append("</table></td></tr>");
        }

        html.append("<tr><td style=\"background-color:#ffffff; padding:16px 32px 8px 32px;\" class=\"mobile-padding\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\" style=\"background:linear-gradient(180deg, #faf7ff 0%, #ffffff 100%); border:1px solid #eadffd; border-radius:20px;\">")
                .append("<tr><td style=\"padding:22px 22px 20px 22px;\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\">")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:20px; line-height:28px; color:#1f2433; font-weight:bold;\">")
                .append("O que você libera ao concluir o pagamento")
                .append("</td></tr>")
                .append("<tr><td style=\"height:12px; line-height:12px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:15px; line-height:26px; color:#525a71;\">")
                .append("• 10 imagens premium personalizadas<br />• Arquivos prontos para postar<br />• Versões sem marca d’água<br />• Visual pensado para valorizar seu serviço")
                .append("</td></tr></table></td></tr></table></td></tr>")

                .append("<tr><td style=\"background-color:#ffffff; padding:18px 32px 8px 32px;\" class=\"mobile-padding\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\" style=\"border:1px solid #dde5f3; border-radius:16px;\">")
                .append("<tr><td style=\"padding:18px 18px 12px 18px;\" class=\"mobile-center\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\">")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:18px; line-height:26px; color:#1f2433; font-weight:bold;\" class=\"mobile-center\">")
                .append("Pagamento seguro e liberação rápida")
                .append("</td></tr>")
                .append("<tr><td style=\"height:10px; line-height:10px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:15px; line-height:24px; color:#5d667d;\" class=\"mobile-center\">")
                .append("Conclua o pagamento para liberar os arquivos originais. Cobrança via ")
                .append(HtmlUtils.htmlEscape(descriptor))
                .append(escapedFormattedPrice != null ? " (" + escapedFormattedPrice + ")" : "")
                .append(".")
                .append("</td></tr>")
                .append("<tr><td style=\"height:16px; line-height:16px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td class=\"mobile-center\">")
                .append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" role=\"presentation\" style=\"border-collapse:separate;\"><tr><td align=\"center\" bgcolor=\"#171b31\" style=\"border-radius:12px;\">");

        if (escapedPaymentUrl != null) {
            html.append("<a href=\"")
                    .append(escapedPaymentUrl)
                    .append("\" target=\"_blank\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:16px; line-height:16px; font-weight:bold; color:#ffffff; text-decoration:none; padding:14px 28px; display:inline-block; border-radius:12px;\">")
                    .append(HtmlUtils.htmlEscape(ctaLabel))
                    .append("</a>");
        } else {
            html.append("<span style=\"font-family:Arial, Helvetica, sans-serif; font-size:16px; line-height:16px; font-weight:bold; color:#ffffff; text-decoration:none; padding:14px 28px; display:inline-block; border-radius:12px;\">")
                    .append(HtmlUtils.htmlEscape(ctaLabel))
                    .append("</span>");
        }

        html.append("</td></tr></table>")
                .append("</td></tr>")
                .append("</table></td></tr>")

                .append("<tr><td style=\"background-color:#ffffff; padding:20px 32px 26px 32px; border-radius:0 0 28px 28px;\" class=\"mobile-padding\">")
                .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\">")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:22px; color:#5d667d;\">")
                .append("<strong>Em caso de dúvidas</strong>, responda este e-mail para falar com nossa equipe.")
                .append("</td></tr>")
                .append("<tr><td style=\"height:12px; line-height:12px; font-size:0;\">&nbsp;</td></tr>")
                .append("<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:12px; line-height:18px; color:#7c8397;\">")
                .append("Pacote ").append(pending.packageId())
                .append(" · Experimento ").append(escapedExperiment)
                .append(pending.sampleUpdatedAt() != null
                        ? " · E-mail atualizado em " + HtmlUtils.htmlEscape(HUMAN_DATE.format(pending.sampleUpdatedAt()))
                        : "")
                .append("</td></tr>")
                .append("</table></td></tr>")
                .append("</table></td></tr></table>");

        if (StringUtils.hasText(trackingPixelUrl)) {
            html.append("<img src=\"")
                    .append(HtmlUtils.htmlEscape(trackingPixelUrl))
                    .append("\" alt=\"\" width=\"1\" height=\"1\" style=\"display:none;\" />");
        }
        html.append("</body></html>");

        String attachmentName = "amostras-com-marca-dagua-" + pending.packageId() + ".zip";
        return new EmailContent(subject, plain.toString(), html.toString(), attachmentName);
    }

    private String buildInlinePreviewCards(List<InlinePreview> inlinePreviews) {
        if (inlinePreviews == null || inlinePreviews.isEmpty()) {
            return "";
        }
        StringBuilder cards = new StringBuilder();
        int renderedCards = 0;
        cards.append("<tr class=\"mobile-stack\">");
        for (int i = 0; i < inlinePreviews.size(); i++) {
            InlinePreview preview = inlinePreviews.get(i);
            if (!StringUtils.hasText(preview.url())) {
                continue;
            }
            PreviewCardStyle style = PREVIEW_CARD_STYLES[i % PREVIEW_CARD_STYLES.length];
            String caption = INLINE_PREVIEW_CAPTIONS[i % INLINE_PREVIEW_CAPTIONS.length];
            cards.append("<td width=\"33.33%\" style=\"padding:8px; vertical-align:top;\">")
                    .append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" role=\"presentation\" style=\"background-color:")
                    .append(style.backgroundColor())
                    .append("; border:1px solid ")
                    .append(style.borderColor())
                    .append("; border-radius:18px; overflow:hidden;\">")
                    .append("<tr><td><img src=\"")
                    .append(HtmlUtils.htmlEscape(preview.url()))
                    .append("\" alt=\"Prévia da arte ")
                    .append(i + 1)
                    .append(" com marca d'água\" width=\"160\" class=\"fluid-img\" style=\"width:100%; max-width:160px; border-radius:18px 18px 0 0;\" /></td></tr>")
                    .append("<tr><td style=\"padding:12px 12px 14px 12px; font-family:Arial, Helvetica, sans-serif; font-size:13px; line-height:19px; color:#51586f;\">")
                    .append(HtmlUtils.htmlEscape(caption))
                    .append("</td></tr></table></td>");
            renderedCards++;
        }
        if (renderedCards == 0) {
            return "";
        }
        cards.append("</tr>");
        return cards.toString();
    }

    private String resolveEmailSubject(PendingPackage pending, int imageCount, String templateSubject) {
        if (StringUtils.hasText(templateSubject)) {
            return templateSubject.trim();
        }

        // Assunto objetivo e transacional (reduz sinais de e-mail promocional)
        String name = StringUtils.hasText(pending.submissionName()) ? pending.submissionName().trim() : null;
        String formattedPrice = formatCurrency(DEFAULT_PRODUCT_PRICE, DEFAULT_PRODUCT_CURRENCY);

        String base = "Amostras anexas: " + imageCount + " imagens com marca d'água";
        if (StringUtils.hasText(name)) {
            base = name + " — " + base;
        }
        return base + " (libere o pacote completo por " + formattedPrice + ")";
    }

    private String resolveExperimentLabel(PendingPackage pending) {
        if (StringUtils.hasText(pending.experimentName())) {
            return pending.experimentName().trim();
        }
        return "Lead Portal";
    }

    private PaymentInfo ensurePaymentInfo(PendingPackage pending) {
        if (isPaymentLinkValid(pending.paymentCheckoutUrl(), pending.paymentCheckoutExpiresAt())) {
            return new PaymentInfo(
                    pending.paymentPurchaseId(),
                    pending.paymentCheckoutUrl(),
                    pending.paymentAmount(),
                    pending.paymentCurrency(),
                    pending.paymentCheckoutExpiresAt(),
                    pending.paymentStatementDescriptor());
        }
        PaymentCheckoutResponse checkout = paymentsClient.ensureCheckout(
                pending.packageId(), pending.submissionEmail(), pending.submissionName());
        if (checkout == null || !StringUtils.hasText(checkout.checkoutUrl())) {
            throw new IllegalStateException("Serviço de pagamentos não retornou link válido para o pacote "
                    + pending.packageId());
        }
        persistPaymentInfo(pending.packageId(), checkout);
        return new PaymentInfo(
                checkout.purchaseId(),
                checkout.checkoutUrl(),
                checkout.amount(),
                checkout.currency(),
                checkout.expiresAt(),
                checkout.statementDescriptor());
    }

    private boolean isPaymentLinkValid(String checkoutUrl, Instant expiresAt) {
        if (!StringUtils.hasText(checkoutUrl)) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        return expiresAt.isAfter(Instant.now());
    }

    private void persistPaymentInfo(long packageId, PaymentCheckoutResponse checkout) {
        Timestamp expiresAt = checkout.expiresAt() != null ? Timestamp.from(checkout.expiresAt()) : null;
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET payment_purchase_id = ?, payment_checkout_url = ?, "
                        + "payment_checkout_expires_at = ?, payment_amount = ?, payment_currency = ?, "
                        + "payment_statement_descriptor = ?, updated_at = ? WHERE id = ?",
                checkout.purchaseId(),
                checkout.checkoutUrl(),
                expiresAt,
                checkout.amount(),
                checkout.currency(),
                checkout.statementDescriptor(),
                Timestamp.from(Instant.now()),
                packageId);
    }

    private void appendPaymentCallToAction(StringBuilder plain,
                                           StringBuilder html,
                                           PaymentInfo paymentInfo,
                                           String suggestedCta) {
        if (paymentInfo == null || !StringUtils.hasText(paymentInfo.checkoutUrl())) {
            return;
        }
        String paymentUrl = paymentInfo.checkoutUrl().trim();

        BigDecimal amount = paymentInfo.amount() != null ? paymentInfo.amount() : DEFAULT_PRODUCT_PRICE;
        String currency = StringUtils.hasText(paymentInfo.currency()) ? paymentInfo.currency() : DEFAULT_PRODUCT_CURRENCY;
        String formattedAmount = formatCurrency(amount, currency);
        String descriptor = StringUtils.hasText(paymentInfo.statementDescriptor())
                ? paymentInfo.statementDescriptor()
                : "Mercado Pago";

        if (plain != null) {
            plain.append("\nLink para pagamento (liberar originais):\n")
                    .append(paymentUrl)
                    .append("\n");
            if (formattedAmount != null) {
                plain.append("Valor: ").append(formattedAmount).append("\n");
            }
            plain.append("Cobrança via ").append(descriptor).append("\n\n");
        }

        String ctaLabel = StringUtils.hasText(suggestedCta)
                ? suggestedCta.trim()
                : "Liberar arquivos originais";
        if (formattedAmount != null) {
            ctaLabel = ctaLabel + " — " + formattedAmount;
        }

        if (html != null) {
            html.append("<div style=\"margin:24px 0;padding:16px 20px;border:1px solid #e3e3e3;border-radius:8px;background:#f8f8f8;\">")
                .append("<p><strong>Conclua o pagamento para liberar os arquivos originais.</strong></p>")
                .append("<p>Cobrança via ")
                .append(HtmlUtils.htmlEscape(descriptor))
                .append("</p>")
                .append("<p style=\"text-align:center;\"><a href=\"")
                .append(HtmlUtils.htmlEscape(paymentUrl))
                .append("\" target=\"_blank\" rel=\"noopener\" style=\"display:inline-block;padding:14px 28px;background:#111;color:#fff;font-weight:600;border-radius:6px;text-decoration:none;\">")
                .append(HtmlUtils.htmlEscape(ctaLabel))
                .append("</a></p>")
                .append("</div>");
        }
    }
    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        Locale locale = "BRL".equalsIgnoreCase(currency) ? new Locale("pt", "BR") : Locale.US;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        try {
            return formatter.format(amount);
        } catch (IllegalArgumentException ignored) {
            return amount.toPlainString() + (StringUtils.hasText(currency) ? " " + currency : "");
        }
    }

    private record TrackingLinks(String viewUrl, String pixelUrl) {
    }

    private record InlinePreview(String url) {
    }

    private record PreviewCardStyle(String backgroundColor, String borderColor) {
    }

    private record PaymentInfo(
            Long purchaseId,
            String checkoutUrl,
            BigDecimal amount,
            String currency,
            Instant expiresAt,
            String statementDescriptor) {
    }


    private void markAsNotified(long packageId, FlowSubmissionImagePackageStatus currentStatus) {
        FlowSubmissionImagePackageStatus nextStatus = currentStatus == FlowSubmissionImagePackageStatus.COMPLETED
                ? currentStatus
                : FlowSubmissionImagePackageStatus.COMPLETED;
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notified_at = ?, notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = NULL, status = ? WHERE id = ?",
                Timestamp.from(now),
                Timestamp.from(now),
                nextStatus.name(),
                packageId);
        if (updated > 0) {
            statusHistoryService.recordStatusChange(packageId, nextStatus, null, now);
        }
    }

    private void recordFailure(long packageId, String error) {
        String normalizedError = normalizeError(error);
        Instant now = Instant.now();

        int updatedAttempts = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = ? WHERE id = ?",
                Timestamp.from(now),
                normalizedError,
                packageId);
        if (updatedAttempts == 0) {
            log.warn("Pacote {} não encontrado ao registrar falha de notificação", packageId);
            return;
        }

        NotificationStatus notificationStatus = findNotificationStatus(packageId).orElse(null);
        if (notificationStatus == null) {
            log.warn("Pacote {} não encontrado ao carregar tentativas de notificação após falha", packageId);
            return;
        }

        int attempts = notificationStatus.notificationAttempts() != null
                ? notificationStatus.notificationAttempts()
                : 0;
        int allowedAttempts = Math.max(1, maxAttempts);
        if (attempts >= allowedAttempts
                && notificationStatus.status() != FlowSubmissionImagePackageStatus.FAILED) {
            String failureReason = buildEmailFailureReason(normalizedError);
            int updated = jdbcTemplate.update(
                    "UPDATE flow_submission_image_package SET status = ?, failure_reason = ?, updated_at = ? WHERE id = ?",
                    FlowSubmissionImagePackageStatus.FAILED.name(),
                    failureReason,
                    Timestamp.from(now),
                    packageId);
            if (updated > 0) {
                statusHistoryService.recordStatusChange(
                        packageId, FlowSubmissionImagePackageStatus.FAILED, failureReason, now);
                log.warn(
                        "Pacote {} marcado como FAILED após {} tentativas de envio de e-mail: {}",
                        packageId,
                        attempts,
                        normalizedError);
            }
        }
    }

    private Optional<FlowSubmissionImagePackageStatus> findStatus(long packageId) {
        String sql = "SELECT status FROM flow_submission_image_package WHERE id = ?";
        List<FlowSubmissionImagePackageStatus> statuses = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> parseStatus(rs.getString("status")),
                packageId);
        return statuses.stream().findFirst();
    }

    private FlowSubmissionImagePackageStatus parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
        try {
            return FlowSubmissionImagePackageStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Optional<NotificationStatus> findNotificationStatus(long packageId) {
        String sql = "SELECT status, notification_attempts FROM flow_submission_image_package WHERE id = ?";
        List<NotificationStatus> statuses = jdbcTemplate.query(sql, (rs, rowNum) -> new NotificationStatus(
                parseStatus(rs.getString("status")),
                (Integer) rs.getObject("notification_attempts")), packageId);
        return statuses.stream().findFirst();
    }


    private String normalizeError(String rawError) {
        String value = StringUtils.hasText(rawError)
                ? rawError.trim()
                : "Motivo não informado pelo serviço de e-mail";
        if (value.length() > 500) {
            value = value.substring(0, 500);
        }
        return value;
    }

    private String normalizeSubmissionId(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof byte[] bytes) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                return new UUID(buffer.getLong(), buffer.getLong()).toString();
            } catch (Exception ex) {
                log.warn("Falha ao converter submission_id binário para UUID", ex);
                return null;
            }
        }
        String value = rawValue.toString();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }

    private String buildEmailFailureReason(String normalizedError) {
        String reason = "Falha ao enviar e-mail: " + normalizedError;
        if (reason.length() > 500) {
            return reason.substring(0, 500);
        }
        return reason;
    }

    private record PendingPackage(
            long packageId,
            String submissionId,
            FlowSubmissionImagePackageStatus status,
            int notificationAttempts,
            Instant lastAttempt,
            String submissionName,
            String submissionEmail,
            Long experimentId,
            String experimentName,
            String sampleSubject,
            String samplePreview,
            String sampleBody,
            String sampleCallToAction,
            String sampleModel,
            String samplePrompt,
            Instant sampleUpdatedAt,
            String zipObjectKey,
            Long zipSizeBytes,
            Instant zipGeneratedAt,
            int imageCount,
            Long paymentPurchaseId,
            String paymentCheckoutUrl,
            Instant paymentCheckoutExpiresAt,
            BigDecimal paymentAmount,
            String paymentCurrency,
            String paymentStatementDescriptor) {
    }

    private record EmailContent(String subject, String plain, String html, String attachmentName) {
    }

    private record NotificationStatus(
            FlowSubmissionImagePackageStatus status,
            Integer notificationAttempts) {
    }

}
